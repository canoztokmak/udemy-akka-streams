package part3_graphs

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

object GraphBasics extends App {
  implicit val system = ActorSystem("GraphBasics")
  implicit val materializer = ActorMaterializer()

  val input = Source(1 to 1000)
  val incrementer = Flow[Int].map(x => x + 1)
  val multiplier = Flow[Int].map(x => x * 10)

  val output = Sink.foreach[(Int, Int)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      // step 2 - add the necessary components of the graph
      val broadcast = builder.add(Broadcast[Int](2))  // fan-out operator
      val zip = builder.add(Zip[Int, Int])                        // fan-in operator

      // step 3 - tying up the components
      input ~> broadcast
      broadcast.out(0) ~> incrementer ~> zip.in0
      broadcast.out(1) ~> multiplier ~> zip.in1
      zip.out ~> output

      // step 4 - return a closed shape
      ClosedShape
    }
  )

//  graph.run()

  /**
   * exercise 1: feed a source into 2 sinks at the same time (hint: broadcast)
   */
//  val multiSinkGraph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//
//      val broadcast = builder.add(Broadcast[Int](2))
//
//      val sink1 = Sink.foreach[Int](x => println(s"Sink1: $x"))
//      val sink2 = Sink.foreach[Int](x => println(s"Sink2: $x"))
//
//      input ~> broadcast ~> sink1
//               broadcast ~> sink2
//
//      ClosedShape
//    }
//  )
//
//  multiSinkGraph.run()

  /**
   * exercise 2:
   * - have 2 flows (fast and slow)
   * - merge them
   * - then separate them
   * - collect with 2 sinks
   */
  val complexGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._
      import scala.concurrent.duration._

      val fastSource = Source(1 to 100).throttle(5, 1 second)
      val slowSource = Source(100 to 200).throttle(2, 1 second)

      val sink1 = Sink.foreach[Int](x => println(s"Sink1: $x"))
      val sink2 = Sink.foreach[Int](x => println(s"Sink2: $x"))

      val merge = builder.add(Merge[Int](2))
      val balance = builder.add(Balance[Int](2))

      fastSource ~> merge
      slowSource ~> merge

      merge ~> balance

      balance ~> sink1
      balance ~> sink2

      ClosedShape
    }
  )

  complexGraph.run()
}
