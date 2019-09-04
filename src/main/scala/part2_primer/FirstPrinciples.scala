package part2_primer

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Flow, Sink, Source}

import scala.concurrent.Future

object FirstPrinciples extends App {

  implicit val system = ActorSystem("FirstPrinciples")
  implicit val materalizer = ActorMaterializer()

  val source = Source(1 to 10)
  val sink = Sink.foreach[Int](println)

  val graph = source.to(sink)
//  graph.run()

  //

  val flow = Flow[Int].map(_ + 1)

  val sourceWithFlow = source.via(flow)
  val flowWithSink = flow.to(sink)

//  sourceWithFlow.to(sink).run()
//  source.to(flowWithSink).run()
//  source.via(flow).to(sink).run()

//  val illegalSource = Source.single[String](null)
//  illegalSource.to(Sink.foreach(println)).run()

  //
  val finiteSource = Source.single(1)
  val anotherFiniteSource = Source(List(1,2,3))
  val emptySource = Source.empty[Int]
  val infiniteSource = Source(Stream.from(1))

  import scala.concurrent.ExecutionContext.Implicits.global
  val futureSource = Source.fromFuture(Future(42))

  //
  val boringSink = Sink.ignore
  val foreachSink = Sink.foreach[String](println)
  val headSink = Sink.head[Int] // retrieves head and closes the stream
  val foldSink = Sink.fold[Int, Int](0)((a, b) => a + b)

  //
  val mapFlow = Flow[Int].map(x => 2 * x)
  val takeFlow = Flow[Int].take(5)

  //
  val doubleFlowGraph = source.via(mapFlow).via(takeFlow).to(sink)
//  doubleFlowGraph.run()

  //
  val mapSource = Source(1 to 10).map(x => x * 2)
//  mapSource.runForeach(println)

  /**
   * create a stream that takes the names of persons, then you will keep the first 2 names with length > 5 chars
   */
  val nameSource = Source(List("can", "cenk", "radamel", "falcao", "duygu", "wiki"))
  val filterFlow = Flow[String].filter(_.length > 5)
  val limitFlow = Flow[String].take(2)
  val printSink = Sink.foreach[String](println)

  nameSource.via(filterFlow).via(limitFlow).to(printSink).run()
  nameSource.filter(_.length > 5).take(2).runForeach(println)
}
