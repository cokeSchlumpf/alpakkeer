package alpakkeer.core.stream

import java.util.concurrent.TimeUnit

import akka.stream._
import akka.stream.scaladsl.{Flow, GraphDSL, Keep, Source}
import akka.stream.stage._
import alpakkeer.core.stream.RequestResponseStage._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

final class RequestResponseStage[Req, Res, Out](
  settings: Settings,
  matcher: (Req, Res) => Boolean,
  combine: (Req, Res) => Out) extends GraphStage[FanInShape2[Req, Res, Try[Out]]] {

  val requests: Inlet[Req] = Inlet[Req]("RequestResponseStage.requests")

  val responses: Inlet[Res] = Inlet[Res]("RequestResponseStage.responses")

  val out: Outlet[Try[Out]] = Outlet[Try[Out]]("RequestResponseStage.out")

  override def shape: FanInShape2[Req, Res, Try[Out]] = new FanInShape2(requests, responses, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {

    private val waitingFor: mutable.ListBuffer[(Long, Req)] = mutable.ListBuffer()

    private val completeResponses: mutable.ListBuffer[Try[Out]] = mutable.ListBuffer()

    private var pulledResponses = false

    setHandler(responses, new InHandler {
      override def onPush(): Unit = {
        val res = grab(responses)
        pulledResponses = false

        waitingFor.indexWhere(req => matcher(req._2, res)) match {
          case idx if idx >= 0 =>
            completeResponses.addOne(Success(combine(waitingFor(idx)._2, res)))
            waitingFor.remove(idx)
          case _ =>
            completeResponses.addOne(Failure(UnexpectedResponseException(res)))
        }

        if (isAvailable(out)) {
          push(out, completeResponses.remove(0))
        }
      }
    })

    setHandler(requests, new InHandler {
      override def onPush(): Unit = {
        val req = grab(requests)
        waitingFor.addOne((System.nanoTime(), req))

        if (isAvailable(out) && !pulledResponses) {
          pull(responses)
          pulledResponses = true
        }
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = {
        if (completeResponses.isEmpty && waitingFor.size < settings.parallelism) {
          pull(requests)
        } else if (completeResponses.isEmpty && !pulledResponses) {
          pull(responses)
        } else if (completeResponses.nonEmpty) {
          push(out, completeResponses.remove(0))
        }
      }
    })

    override def preStart(): Unit = {
      super.preStart()
      scheduleAtFixedRate("Tick", settings.interval, settings.interval)
    }

    override protected def onTimer(timerKey: Any): Unit = {
      super.onTimer(timerKey)

      val now = System.nanoTime()
      val timeoutNanos = settings.timeout.toNanos

      waitingFor.filterInPlace({
        case (time, req) =>
          val wait = (now - time) < timeoutNanos
          if (!wait) completeResponses.addOne(Failure(ResponseTimeoutException(req)))
          wait
      })
    }

  }

}

object RequestResponseStage {

  case class UnexpectedResponseException[Res](response: Res) extends RuntimeException {

    override def toString: String = {
      s"UnexpectedResponseException($response)"
    }

  }

  case class ResponseTimeoutException[Req](response: Req) extends RuntimeException {

    override def toString: String = {
      s"ResponseTimeoutException($response)"
    }

  }

  case class Settings(parallelism: Int = 5, timeout: FiniteDuration = FiniteDuration.apply(10, TimeUnit.SECONDS), interval: FiniteDuration = FiniteDuration.apply(5, TimeUnit.SECONDS))

  def createWithMappedResponse[Req, Res, Out, Mat](
    response: Source[Res, Mat],
    matcher: (Req, Res) => Boolean,
    combine: (Req, Res) => Out,
    settings: Settings = Settings()): Flow[Req, Try[Out], Mat] = {


    Flow.fromGraph(GraphDSL.create(response)({ implicit b =>
      responseShape => {
        import GraphDSL.Implicits._

        val requests = b.add(Flow[Req])
        val combined = b.add(new RequestResponseStage[Req, Res, Out](settings, matcher, combine))

        requests.out ~> combined.in0
        responseShape.out ~> combined.in1

        FlowShape(requests.in, combined.out)
      }
    }))
  }

  def create[Req, Res, Mat](
    response: Source[Res, Mat],
    matcher: (Req, Res) => Boolean,
    settings: Settings = Settings()): Flow[Req, Try[Res], Mat] = {

    createWithMappedResponse[Req, Res, Res, Mat](response, matcher, Keep.right, settings)
  }

}

/*
object TestApp extends App {

  implicit val system: ActorSystem = ActorSystem()

  val requests: Source[Int, NotUsed] = Source(1 to 100)
  val responses: Source[Int, NotUsed] = Source(1 to 100)

  val done: Future[Done] = requests
    .via(create[Int, Int, NotUsed](responses, (a, b) => a == b, Settings()))
    .toMat(Sink.foreach(println))(Keep.right)
    .run()

  Await.result(done, scala.concurrent.duration.Duration.Inf)
  system.terminate()

}
*/