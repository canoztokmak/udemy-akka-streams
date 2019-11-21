package part3_graphs

import java.util.Date

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape, FanOutShape2, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}

object MoreOpenGraphs extends App {

  implicit val system = ActorSystem("MoreOpenGraphs")
  implicit val mat = ActorMaterializer()

  val max3StaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val max1 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))
    val max2 = builder.add(ZipWith[Int, Int, Int]((a,b) => Math.max(a,b)))

    max1.out ~> max2.in0

    UniformFanInShape(max2.out, max1.in0, max1.in1, max2.in1)
  }

  val source1 = Source(1 to 10)
  val source2 = Source((1 to 10).map(_ => 5))
  val source3 = Source((1 to 10).reverse)

  val maxSink = Sink.foreach[Int](x => println(s"Max is $x"))

  val max3RunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val max3Shape = builder.add(max3StaticGraph)

      source1 ~> max3Shape.in(0)
      source2 ~> max3Shape.in(1)
      source3 ~> max3Shape.in(2)
      max3Shape.out ~> maxSink

      ClosedShape
    }
  )

//  max3RunnableGraph.run()

  /*
    non-uniform fan out shape

    processing bank transactions
    if txn > 10000 suspicious

    streams component for txns
    - output1: let the txn go through
    - output2: suspicious txn ids
   */

  case class Transaction(id: String, source: String, recepient: String, amount: Int, date: Date)

  val transactionSource = Source[Transaction](List(
    Transaction("34324234", "Paul", "Jim", 100, new Date),
    Transaction("72847239", "Daniel", "Jim", 100000, new Date),
    Transaction("12310273", "Jim", "Alice", 7000, new Date),
  ))

  val bankProcessor = Sink.foreach[Transaction](println)

  val suspiciousAnalysisService = Sink.foreach[String](txnId => println(s"Suspicious txnId: $txnId"))

  val suspiciousTxnStaticGraph = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    // define SHAPES
    val broadcast = builder.add(Broadcast[Transaction](2))
    val suspiciousTxnFilter = builder.add(Flow[Transaction].filter(_.amount > 10000))
    val txnIdExtractor = builder.add(Flow[Transaction].map[String](_.id))

    // tie SHAPES
    broadcast.out(0) ~> suspiciousTxnFilter ~> txnIdExtractor

    new FanOutShape2(broadcast.in, broadcast.out(1), txnIdExtractor.out)
  }

  val suspiciousTxnRunnableGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val suspiciousTxnShape = builder.add(suspiciousTxnStaticGraph)

      transactionSource ~> suspiciousTxnShape.in
      suspiciousTxnShape.out0 ~> bankProcessor
      suspiciousTxnShape.out1 ~> suspiciousAnalysisService

      ClosedShape
    }
  )

  suspiciousTxnRunnableGraph.run()
}
