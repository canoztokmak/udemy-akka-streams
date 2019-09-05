package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, FlowShape, SinkShape, SourceShape}
import akka.stream.scaladsl.{Broadcast, Concat, Flow, GraphDSL, Sink, Source}

object OpenGraphs extends App {
  implicit val actorSystem = ActorSystem("OpenGraphs")
  implicit val materializer = ActorMaterializer()

  /**
   * A composite source that concatenates 2 sources
   * - emits ALL the elements from the first source
   * - then ALL the elements from the second
   */
  val firstSource = Source(1 to 10)
  val secondSource = Source(42 to 1000)

  val sourceGraph = Source.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val concat = builder.add(Concat[Int](2))

      firstSource ~> concat
      secondSource ~> concat

      SourceShape(concat.out)
    }
  )

//  sourceGraph.runWith(Sink.foreach[Int](println))

  /**
   * A complex sink
   */
  val sink1 = Sink.foreach[Int](x => println(s"Meaningful thing 1: $x"))
  val sink2 = Sink.foreach[Int](x => println(s"Meaningful thing 2: $x"))

  val sinkGraph = Sink.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val broadcast = builder.add(Broadcast[Int](2))

      broadcast ~> sink1
      broadcast ~> sink2

      SinkShape(broadcast.in)
    }
  )

//  firstSource.to(sinkGraph).run()

  /**
   * Challenge - complex flow
   * Write your own flow that's composed of two other flows
   * - one that adds 1 to a number
   * - one that does number * 10
   */

  val flowGraph = Flow.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val flow1 = Flow[Int].map(_ + 1)
      val flow2 = Flow[Int].map(_ * 10)

      val incrementerShape = builder.add(flow1)
      val multiplierShape = builder.add(flow2)

      incrementerShape ~> multiplierShape

      FlowShape(incrementerShape.in, multiplierShape.out)
    }
  )

  firstSource.via(flowGraph).to(Sink.foreach[Int](println)).run()
}
