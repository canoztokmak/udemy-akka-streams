package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.util.{Failure, Success}

object MaterializingStreams extends App {

  implicit val system = ActorSystem("MaterializingStreams")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val simpleGraph = Source(1 to 10).to(Sink.foreach(println))
//  val simpleMaterializedValue = simpleGraph.run()

  val source = Source(1 to 10)
  val sink = Sink.reduce[Int]((a, b) => a + b)

//  val sumFuture = source.runWith(sink)

//  sumFuture.onComplete {
//    case Success(value) => println(s"the sum is $value")
//    case Failure(exception) => println(s"the sum failed with $exception")
//  }

  val simpleSource = Source(1 to 10)
  val simpleFlow = Flow[Int].map(x => x + 1)
  val simpleSink = Sink.foreach[Int](println)

//  val graph = simpleSource
//    .viaMat(simpleFlow)(Keep.right)
//    .toMat(simpleSink)(Keep.right)
//
//  graph.run().onComplete {
//    case Success(_) => println("processing completed..")
//    case Failure(e) => println(s"processing failed with $e")
//  }

  //
//  Source(1 to 10).runWith(Sink.reduce[Int](_ + _)) // source.to(Sink.reduce)(Keep.right)
//  Source(1 to 10).runReduce[Int](_ + _)  // same

  // backwards
//  Sink.foreach[Int](println).runWith(Source.single(42)) // source(...).to(sink...).run()
  // both ways
//  Flow[Int].map(x => 2 * x).runWith(simpleSource, simpleSink)

  /**
   * - return the last element of a source (use Sink.last)
   * - compute the total word count out of a stream of sentences
   *    - map, fold, reduce
   */
  val lastValue = Source(1 to 10).runWith(Sink.last)
  lastValue.onComplete {
    case Success(last) => println(s"last value is $last")
    case Failure(e) => println(s"last value calculation is failed with $e")
  }

  val stream = List("return the last element of a source (use Sink.last)", "compute the total word count out of a stream of sentences", "map, fold, reduce").toStream
  val sentencesSource = Source(stream)
  val flowSplit = Flow[String].map(_.split(" "))
    .fold(0)((sum, words) => sum + words.length)

  val result = sentencesSource.via(flowSplit).toMat(Sink.reduce[Int](_ + _))(Keep.right).run()
  result.onComplete {
    case Success(totalWords) => println(s"total number of words is $totalWords")
    case Failure(e) => println(s"stream ended with $e")
  }

}
