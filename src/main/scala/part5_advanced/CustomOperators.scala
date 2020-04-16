package part5_advanced

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Attributes, FlowShape, Inlet, Outlet, SinkShape, SourceShape}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.stage.{GraphStage, GraphStageLogic, GraphStageWithMaterializedValue, InHandler, OutHandler}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Random, Success}

object CustomOperators extends App {

  implicit val system = ActorSystem("CustomOperators")
  implicit val materializer = ActorMaterializer()

  // 1 - custom source emits random numbers until cancelled
  class RandomNumberGenerator(max: Int) extends GraphStage[/* step 0: define the shape */SourceShape[Int]] {
    private val random = new Random()
    // step 1: define the ports and the component-specific members
    val outPort = Outlet[Int]("randomGenerator")

    // step 2: construct a new shape
    override def shape: SourceShape[Int] = SourceShape(outPort)

    // step 3: create the logic
    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      // step 4:
      // define mutable state
      // implement my logic here..
      setHandler(outPort, new OutHandler {
        // when there is demand from downstream
        override def onPull(): Unit = {
          // emit a new element
          val elem = random.nextInt(max)
          push(outPort, elem)
        }
      })
    }
  }

  val randomGeneratorSource = Source.fromGraph(new RandomNumberGenerator(100))

//  randomGeneratorSource.runWith(Sink.foreach(println))

  // 2 - a custom sink that prints elements in batches of a given size
  class Batcher(batchSize: Int) extends GraphStage[SinkShape[Int]] {
    val inPort = Inlet[Int]("batcher")

    override def shape: SinkShape[Int] = SinkShape[Int](inPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      override def preStart(): Unit = {
        pull(inPort)
      }

      // mutable state
      val batch = new mutable.Queue[Int]

      setHandler(inPort, new InHandler {
        // when the upstream wants to send me an element
        override def onPush(): Unit = {
          val elem = grab(inPort)
          batch.enqueue(elem)

          // assume some complex computation
          Thread.sleep(100)
          if (batch.size >= batchSize) {
            println(s"new batch ${batch.dequeueAll(_ => true).mkString("[", ",", "]")}")
          }

          pull(inPort) // send demand upstream
        }

        override def onUpstreamFinish(): Unit = {
          if (batch.nonEmpty) {
            println(s"new batch ${batch.dequeueAll(_ => true).mkString("[", ",", "]")}")
            println("stream finished..")
          }
        }
      })
    }
  }

  val batcherSink = Sink.fromGraph(new Batcher(10))

//  randomGeneratorSource.to(batcherSink).run()

  /**
   * Exercise: a custom flow - a simple filter flow
   * - 2 ports (input and output)
   */
  class SimpleFilter[T](predicate: T => Boolean) extends GraphStage[FlowShape[T, T]] {
    val inPort = Inlet[T]("filterIn")
    val outPort = Outlet[T]("filterOut")

    override def shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {
      setHandler(inPort, new InHandler {
        override def onPush(): Unit = {
          try {
            val elem = grab(inPort)

            if (predicate(elem)) push(outPort, elem)
            else pull(inPort)
          } catch {
            case th: Throwable =>
              failStage(th)
          }
        }
      })

      setHandler(outPort, new OutHandler {
        override def onPull(): Unit = {
          pull(inPort)
        }
      })
    }
  }

//  randomGeneratorSource.via(new SimpleFilter[Int](_ > 50)).to(batcherSink).run()

  /**
   * materialized values in graph stages
   */

  // 3 - a flow that counts the number of elements that go through it
  class CounterFlow[T] extends GraphStageWithMaterializedValue[FlowShape[T, T], Future[Int]] {
    val inPort = Inlet[T]("counterIn")
    val outPort = Outlet[T]("counterOut")

    override val shape: FlowShape[T, T] = FlowShape(inPort, outPort)

    override def createLogicAndMaterializedValue(inheritedAttributes: Attributes): (GraphStageLogic, Future[Int]) = {
      val promise = Promise[Int]

      val logic = new GraphStageLogic(shape) {
        // set mutable state inside the logic
        var counter = 0

        setHandler(outPort, new OutHandler {
          override def onPull(): Unit = pull(inPort)

          override def onDownstreamFinish(): Unit = {
            promise.success(counter)
            super.onDownstreamFinish()
          }
        })

        setHandler(inPort, new InHandler {
          override def onPush(): Unit = {
            val elem = grab(inPort)
            counter = counter + 1
            push(outPort, elem)
          }

          override def onUpstreamFinish(): Unit = {
            promise.success(counter)
            super.onUpstreamFinish()
          }

          override def onUpstreamFailure(ex: Throwable): Unit = {
            promise.failure(ex)
            super.onUpstreamFailure(ex)
          }
        })
      }

      (logic, promise.future)
    }
  }

  val counterFlow = Flow.fromGraph(new CounterFlow[Int])
  val countFuture = Source(1 to 10)
//    .map(x => if (x > 7) throw new RuntimeException("gotcha!!") else x)
    .viaMat(counterFlow)(Keep.right)
    .to(Sink.foreach[Int](println))
//    .to(Sink.foreach[Int](x => if (x == 7) throw new RuntimeException("gotcha!!") else x))
    .run()

  countFuture.onComplete {
    case Success(value) => println(s"number of items passed through: $value")
    case Failure(exception) => println(s"stream failed: $exception")
  }(system.dispatcher)
}
