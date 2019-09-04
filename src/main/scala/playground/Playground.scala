package playground

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Source, Sink}

object Playground extends App {

  implicit val actorSystem = ActorSystem("Playground")
  implicit val materializer = ActorMaterializer()

  Source.single("hello, Streams !").to(Sink.foreach(println)).run()

}
