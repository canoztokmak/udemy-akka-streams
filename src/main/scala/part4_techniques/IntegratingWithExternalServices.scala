package part4_techniques

import java.util.Date

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import akka.util.Timeout

import scala.concurrent.Future

object IntegratingWithExternalServices extends App {

  implicit val system = ActorSystem("IntegratingWithExternalServices")
  implicit val materializer = ActorMaterializer()
//  import system.dispatcher // not recommended for mapAsync
  implicit val dispatcher = system.dispatchers.lookup("dedicated-dispatcher")

  def genericExternalService[A, B](element: A): Future[B] = ???

  // example: Simplified PagerDuty
  case class PagerEvent(application: String, description: String, date: Date)

  val eventSource = Source(List(
    PagerEvent("AkkaInfra", "infra broke", new Date),
    PagerEvent("FastData", "illegal elements", new Date),
    PagerEvent("AkkaInfra", "a service stopped", new Date),
    PagerEvent("SuperFrontEnd", "a button doesnt work", new Date)
  ))

  object PagerService {
    private val engineers = List("Daniel", "John", "Gaga")
    private val emails = Map[String, String](
      "Daniel" -> "daniel@hede.com",
      "John" -> "john@hede.com",
      "Gaga" -> "gaga@hede.com"
    )

    def processEvent(pagerEvent: PagerEvent) = Future {
      val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length

      val engineer = engineers(engineerIndex.toInt)

      val email = emails(engineer)

      // page the engineer
      println(s"Sending engineer $email notification: $pagerEvent")
      Thread.sleep(1000)

      email
    }
  }

  val infraEvents = eventSource.filter(_.application == "AkkaInfra")
  val pagedEngineerEmails = infraEvents.mapAsync(1)(event => PagerService.processEvent(event))
  val pagedEmailsSink = Sink.foreach[String](email => println(s"Successfully sent email to $email"))

//  pagedEngineerEmails.to(pagedEmailsSink).run()

  class PagerActor extends Actor with ActorLogging {

    private val engineers = List("Daniel", "John", "Gaga")
    private val emails = Map[String, String](
      "Daniel" -> "daniel@hede.com",
      "John" -> "john@hede.com",
      "Gaga" -> "gaga@hede.com"
    )

    private def processEvent(pagerEvent: PagerEvent) = {
      val engineerIndex = pagerEvent.date.toInstant.getEpochSecond / (24 * 3600) % engineers.length

      val engineer = engineers(engineerIndex.toInt)

      val email = emails(engineer)

      // page the engineer
      log.info(s"Sending engineer $email notification: $pagerEvent")
      Thread.sleep(1000)

      email
    }

    override def receive: Receive = {
      case pagerEvent: PagerEvent =>
        sender() ! processEvent(pagerEvent)

      case _ =>
    }
  }

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val timeout = Timeout(3 seconds)
  val pagerActor = system.actorOf(Props[PagerActor], "pagerActor")
  val alternativePagedEngineerEmails = infraEvents.mapAsync(4)(event => (pagerActor ? event).mapTo[String])
  alternativePagedEngineerEmails.to(pagedEmailsSink).run()
}
