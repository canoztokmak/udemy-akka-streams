package part5_advanced

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Keep, Sink, Source}

import scala.util.{Failure, Success}

object Substreams extends App {

  implicit val system = ActorSystem("Substreams")
  implicit val materializer = ActorMaterializer()

  // 1 - grouping a stream by a certain function
  val wordsSource = Source(List("akka", "is", "amazing", "learning", "substreams"))
  val groups = wordsSource.groupBy(30, word => if(word.isEmpty) '\0' else word.toLowerCase().charAt(0))

  groups.to(Sink.fold(0)((count, word) => {
    val newCount = count + 1
    println(s"I just received $word, count is $newCount")
    newCount
  }))
    .run()

  // 2 - merge substreams back
  val textSource = Source(List(
    "i love akka streams",
    "this is amazing",
    "learning from rock the jvm"
  ))

  val totalCharCountFuture = textSource.groupBy(2, string => string.length % 2)
    .map(_.length)
    .mergeSubstreamsWithParallelism(2)
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  totalCharCountFuture.onComplete {
    case Success(value) => println(s"total count is $value")
    case Failure(th) => println(s"computation failed: $th")
  }(system.dispatcher)

  // 3 - splitting a stream into substreams, when a condition is met
  val text =
    "i love akka streams\n" +
    "this is amazing\n" +
    "learning from rock the jvm\n"

  val anotherCharCountFuture = Source(text.toList)
    .splitWhen(_ == '\n')
    .filter(_ != '\n')
    .map(_ => 1)
    .mergeSubstreams
    .toMat(Sink.reduce[Int](_ + _))(Keep.right)
    .run()

  anotherCharCountFuture.onComplete {
    case Success(value) => println(s"total alternative count is $value")
    case Failure(th) => println(s"computation failed: $th")
  }(system.dispatcher)

  // 4 - flattening
  val simpleSource = Source(1 to 5)
  simpleSource.flatMapConcat(x => Source(x to 3*x)).runWith(Sink.foreach(println))
  simpleSource.flatMapMerge(5, x => Source(x to 3*x)).runWith(Sink.foreach(println))

}
