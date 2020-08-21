package alpakkeer.core.stream

import java.time.Instant

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._
import alpakkeer.core.stream.CheckpointMonitor._

import scala.concurrent.duration.FiniteDuration

/**
 * Based on https://github.com/ruippeixotog/akka-stream-mon.
 *
 * A graph stage measuring the element throughput at a given point in a graph. The stage emits through `out` all
 * elements received at `in`, while a second output port `statsOut` emits statistics of the number of elements passing
 * per unit of time at the `in`-`out` edge.
 *
 * `statsOut` emits continuously as demanded by downstream; the connected `Sink` is responsible for throttling demand,
 * controlling that way the update frequency of the stats (or, equivalently, the size of the buckets they represent).
 *
 * @tparam A the type of the elements passing through this stage
 */
final class CheckpointMonitor[A] extends GraphStage[FanOutShape2[A, A, Stats]] {

  private val in: Inlet[A] = Inlet[A]("ThroughputMonitor.in")
  private val out: Outlet[A] = Outlet[A]("ThroughputMonitor.out")
  private val statsOut: Outlet[Stats] = Outlet[Stats]("ThroughputMonitor.statsOut")

  override val shape = new FanOutShape2[A, A, Stats](in, out, statsOut)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private var lastStatsPull = 0L
    private var lastPulled = 0L
    private var lastPushed = 0L

    private var totalPullPushLatencySinceLastStatsPull = 0L
    private var totalPushPullLatencySinceLastStatsPull = 0L
    private var elementsSinceLastPull = 0L

    override def preStart(): Unit = {
      val now = System.nanoTime()
      lastStatsPull = now
      lastPulled = now
      lastPushed = now
    }

    def pushStats(): Unit = {
      val startTime = lastStatsPull
      val endTime = System.nanoTime()
      push(statsOut, Stats(
        Instant.now(),
        endTime - startTime, elementsSinceLastPull,
        totalPushPullLatencySinceLastStatsPull, totalPullPushLatencySinceLastStatsPull))

      lastStatsPull = endTime
      elementsSinceLastPull = 0
      totalPushPullLatencySinceLastStatsPull = 0
      totalPullPushLatencySinceLastStatsPull = 0
    }

    setHandler(in, new InHandler {
      def onPush(): Unit = {
        push(out, grab(in))

        elementsSinceLastPull += 1
        lastPushed = System.nanoTime()
        totalPullPushLatencySinceLastStatsPull += (lastPushed - lastPulled)
      }
    })

    setHandler(out, new OutHandler {
      def onPull(): Unit = {
        pull(in)

        lastPulled = System.nanoTime()
        totalPushPullLatencySinceLastStatsPull += (lastPulled - lastPushed)
      }
    })

    setHandler(statsOut, new OutHandler {
      def onPull(): Unit = pushStats()

      override def onDownstreamFinish(cause: Throwable): Unit = {}
    })
  }
}

object CheckpointMonitor {

  /**
   * Aggregate throughput metrics of a stream.
   *
   * @param timeElapsedNanos the time elapsed between the measurement start and its end, in nanoseconds
   * @param count            the number of elements that passed through the stream
   */
  case class Stats(
    moment: Instant,
    timeElapsedNanos: Long,
    count: Long,
    totalPushPullLatencyNanos: Long,
    totalPullPushLatencyNanos: Long) {

    /**
     * The number of elements that passed through the stream per second.
     */
    def throughputElementsPerSecond: Double = {
      if (count > 0) {
        count.toDouble * (1 / (timeElapsedNanos.toDouble / 100_000_0000d))
      } else {
        0
      }
    }

    /**
     * The average push-pull latency in milliseconds for the last interval
     */
    def pushPullLatencyNanos: Long = {
      if (count > 0) {
        totalPushPullLatencyNanos / count
      } else {
        0
      }
    }

    /**
     * The average pull-push latency in milliseconds for the last interval
     */
    def pullPushLatencyNanos: Long = {
      if (count > 0) {
        totalPullPushLatencyNanos / count
      } else {
        0
      }
    }

    /**
     * String representation
     */
    override def toString: String = {
      s"Stats(timeElapsedNanos: $timeElapsedNanos, count: $count, totalPushPullLatency: $totalPushPullLatencyNanos, " +
        s"totalPullPushLatencyNanos: $totalPullPushLatencyNanos, throughputElementsPerSecond: $throughputElementsPerSecond, " +
        s"pushPullLatencyNanos: $pushPullLatencyNanos, pullPushLatencyNanos: $pullPushLatencyNanos)"
    }

  }

  /**
   * Creates a `ThroughputMonitor` stage.
   *
   * @tparam A the type of the elements passing through this stage
   * @return a `ThroughputMonitor` stage.
   */
  private def apply[A]: CheckpointMonitor[A] =
    new CheckpointMonitor[A]

  /**
   * Creates a `ThroughputMonitor` stage with throughput stats consumed by a given `Sink`.
   *
   * @param statsSink the `Sink` that will consume throughput statistics
   * @tparam A   the type of the elements passing through this stage
   * @tparam Mat the value materialized by `statsSink`
   * @return a `Flow` that outputs all its inputs and emits throughput stats to `statsSink`.
   */
  def apply[A, Mat](statsSink: Sink[Stats, Mat]): Flow[A, A, Mat] = {
    Flow.fromGraph(GraphDSL.create(statsSink) { implicit b =>
      sink =>
        import GraphDSL.Implicits._
        val mon = b.add(apply[A])
        mon.out1 ~> sink
        FlowShape(mon.in, mon.out0)
    })
  }

  /**
   * Creates a `ThroughputMonitor` stage with throughput stats handled periodically by a callback.
   *
   * @param statsInterval the update frequency of the throughput stats
   * @param onStats       the function to call when a throughput stats bucket is available
   * @tparam A the type of the elements passing through this stage
   * @return a `Flow` that outputs all its inputs and calls `onStats` frequently with throughput stats.
   */
  def apply[A](statsInterval: FiniteDuration, onStats: Stats => Unit): Flow[A, A, NotUsed] =
    apply(Flow.fromGraph(new Pulse[Stats](statsInterval)).to(Sink.foreach(onStats)).mapMaterializedValue(_ => NotUsed))

}

