package part2_primer

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Flow, Sink, Source}

object BackpressureBasics extends App {

  implicit val actorSystem = ActorSystem("BackpressureBasics")
  implicit val materializer = ActorMaterializer()

  val fastSource = Source(1 to 1000)
  val slowSink = Sink.foreach[Int] { x =>
    Thread.sleep(1000)
    println(s"Sink: $x")
  }

//  fastSource.async
//    .to(slowSink)
//    .run()

  val simpleFlow = Flow[Int].map { x =>
    println(s"Incoming: $x")
    x + 1
  }

//  fastSource.async
//    .via(simpleFlow).async
//    .to(slowSink)
//    .run()

  /**
   * reactions to backpressure
   * - try to slow down
   * - buffer elements until there's more demand
   * - drop down elements from the buffer if it overflows
   * - tear down/kill the whole stream
   */

//  val bufferedFlow = simpleFlow.buffer(10, OverflowStrategy.dropTail)
//  fastSource.async
//    .via(bufferedFlow).async
//    .to(slowSink)
//    .run()

  // throttling
  import scala.concurrent.duration._
  fastSource.throttle(2, 1 second).runWith(Sink.foreach(println))
}
