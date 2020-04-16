package part5_advanced

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{BroadcastHub, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches}

import scala.concurrent.duration._

object DynamicStreamHandling extends App {

  implicit val system = ActorSystem("DynamicStreamHandling")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

//  // #1: Kill Switch
//  val killSwitchFlow = KillSwitches.single[Int]
//  val counter = Source(Stream.from(1)).throttle(1, 1 second).log("counter")
//  val sink = Sink.ignore
//
//  val killSwitch = counter
//    .viaMat(killSwitchFlow)(Keep.right)
//    .to(sink)
//    .run()
//
//  system.scheduler.scheduleOnce(3 seconds){
//    killSwitch.shutdown()
//  }
//
//  val anotherCounter = Source(Stream.from(1)).throttle(2, 1 second).log("anotherCounter")
//
//  val sharedKillSwitch = KillSwitches.shared("shared")
//
//  counter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//  anotherCounter.via(sharedKillSwitch.flow).runWith(Sink.ignore)
//
//  system.scheduler.scheduleOnce(4 seconds) {
//    sharedKillSwitch.shutdown()
//  }
//
//  // #2: MergeHub
//  val dynamicMerge = MergeHub.source[Int]
//  val materializedSink: Sink[Int, NotUsed] = dynamicMerge.to(Sink.foreach[Int](println)).run()
//
//  // use this sink any time we like
//  Source(1 to 10).runWith(materializedSink)
//  counter.runWith(materializedSink)
//
//  // #3: BroadcastHub
//  val dynamicBroadcast = BroadcastHub.sink[Int]
//  val materializedSource: Source[Int, NotUsed] = Source(1 to 100).runWith(dynamicBroadcast)
//
//  materializedSource.runWith(Sink.ignore)
//  materializedSource.runWith(Sink.foreach[Int](println))

  /**
   *  Challenge - combine a MergeHub and a BroadcastHub
   *  a publisher-subscriber component
   */
  val myMergeHub = MergeHub.source[String]
  val myBroadcastHub = BroadcastHub.sink[String]

  val (publisherPort, subscriberPort) = myMergeHub.toMat(myBroadcastHub)(Keep.both).run()
  subscriberPort.runWith(Sink.foreach(elem => println(s"I received the $elem")))
  subscriberPort.map(_.length).runWith(Sink.foreach(n => println(s"I got a number: $n")))

  Source(List("Akka", "is", "amazing")).runWith(publisherPort)
  Source(List("I", "love", "scala")).runWith(publisherPort)
  Source.single("STREAAAAMMSSSS").runWith(publisherPort)
}
