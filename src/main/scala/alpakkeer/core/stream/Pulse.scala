package alpakkeer.core.stream

import scala.concurrent.duration.{Duration, FiniteDuration}
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}

/**
 * Based on https://github.com/akka/akka-stream-contrib/blob/v0.9/contrib/src/main/scala/akka/stream/contrib/Pulse.scala.
 */
final class Pulse[T](interval: FiniteDuration, initiallyOpen: Boolean = false)
  extends GraphStage[FlowShape[T, T]] {

  val in: Inlet[T] = Inlet[T]("Pulse.in")
  val out: Outlet[T] = Outlet[T]("Pulse.out")
  val shape: FlowShape[T, T] = FlowShape(in, out)

  def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new TimerGraphStageLogic(shape) with InHandler with OutHandler {

      setHandlers(in, out, this)

      override def preStart(): Unit = if (!initiallyOpen) startPulsing()
      override def onPush(): Unit = if (isAvailable(out)) push(out, grab(in))
      override def onPull(): Unit = if (!pulsing) {
        pull(in)
        startPulsing()
      }

      override protected def onTimer(timerKey: Any): Unit = {
        if (isAvailable(out) && !isClosed(in) && !hasBeenPulled(in)) pull(in)
      }

      private def startPulsing(): Unit = {
        pulsing = true
        scheduleAtFixedRate("PulseTimer", interval, interval)
      }

      private var pulsing = false
    }

  override def toString = "Pulse"

}

