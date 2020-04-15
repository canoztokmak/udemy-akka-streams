package part4_techniques

import akka.actor.{Actor, ActorLogging, ActorSystem, PoisonPill, Props}
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.Timeout

import scala.concurrent.duration._

object IntegratingWithActors extends App {

  implicit val system = ActorSystem("IntegratingWithActors")
  implicit val materializer = ActorMaterializer()

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case s: String =>
        log.info(s"Just received a string: $s")

        sender() ! s"$s$s"

      case n: Int =>
        log.info(s"Just received a number: $n")

        sender() ! (2 * n)

      case _ =>
    }
  }

  val simpleActor = system.actorOf(Props[SimpleActor], "simpleActor")

  val numbersSource = Source(1 to 10)

  // actor as a flow
  implicit  val timeout = Timeout(2 seconds)
  val actorBasedFlow = Flow[Int].ask[Int](4)(simpleActor)

//  numbersSource.via(actorBasedFlow).to(Sink.ignore).run()
//  numbersSource.ask[Int](4)(simpleActor).to(Sink.ignore).run()  // equivalent

  /*
    Actor as a source
   */
//  val actorPoweredSource = Source.actorRef[Int](10, OverflowStrategy.dropHead)
//
//  val materializedActorRef = actorPoweredSource.to(Sink.foreach[Int](n => println(s"Actor powered flow got number: $n"))).run()
//
//  materializedActorRef ! 10
//  // terminating the stream
//  materializedActorRef ! akka.actor.Status.Success("complete")

  /*
    Actor as a destination/sink
    - an init message
    - an ack message to confirm the reception
    - a complete message
    - a function to generate a message in case the stream throws an exception
   */

  case object StreamInit
  case object StreamAck
  case object StreamComplete
  case class StreamFailed(th: Throwable)

  class DestinationActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case StreamInit =>
        log.info("Stream initialized")
        sender() ! StreamAck

      case StreamComplete =>
        log.info("Stream complete")
        context.stop(self)

      case StreamFailed(th) =>
        log.warning(s"Stream failed: $th")

      case msg =>
        log.info(s"Message $msg has come to its final resting point.")
        sender() ! StreamAck
    }
  }

  val destinationActor = system.actorOf(Props[DestinationActor], "destinationActor")

  val actorPoweredSink = Sink.actorRefWithAck[Int](
    destinationActor,
    onInitMessage = StreamInit,
    ackMessage = StreamAck,
    onCompleteMessage = StreamComplete,
    onFailureMessage = th => StreamFailed(th)
  )

  Source(1 to 10).to(actorPoweredSink).run()
}
