package part3_graphs

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, BidiShape, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}

object BidirectionalFlows extends App {
  implicit val system = ActorSystem("BidirectionalFlows")
  implicit val mat = ActorMaterializer()

  def encrypt(n: Int)(string: String) = string.map(c => (c + n).toChar)
  def decrypt(n: Int)(string: String) = string.map(c => (c - n).toChar)

  // bidiFlow
  val bidiGraph = GraphDSL.create() { implicit builder =>
    val encFlowShape = builder.add(Flow[String].map(encrypt(3)))
    val decFlowShape = builder.add(Flow[String].map(decrypt(3)))

//    BidiShape(encFlowShape.in, encFlowShape.out, decFlowShape.in, decFlowShape.out)
    BidiShape.fromFlows(encFlowShape, decFlowShape)
  }

  val unencryptedStrings = List("akka", "is", "awesome", "testing", "bidi", "flows")
  val unencryptedSource = Source(unencryptedStrings)

  val encryptedSource = Source(unencryptedStrings.map(encrypt(3)))

  val cryptoBidiGraph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val unecryptedSourceShape = builder.add(unencryptedSource)
      val ecryptedSourceShape = builder.add(encryptedSource)

      val bidi = builder.add(bidiGraph)
      val encryptedSinkShape = Sink.foreach[String](s => println(s"Encrypted: $s"))
      val decryptedSinkShape = Sink.foreach[String](s => println(s"Decrypted: $s"))

      unecryptedSourceShape ~> bidi.in1 ; bidi.out1 ~> encryptedSinkShape
//      ecryptedSourceShape ~> bidi.in2   ; bidi.out2 ~> decryptedSinkShape

      decryptedSinkShape <~ bidi.out2 ; bidi.in2 <~ ecryptedSourceShape

      ClosedShape
    }
  )

  cryptoBidiGraph.run()
}
