package demo

import akka.actor._
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}
import java.util.UUID
import scala.concurrent.Future
import akka.persistence.PersistentActor
import scala.concurrent.duration._
import akka.pattern.ask
import scala.concurrent.Await
import akka.util.Timeout
import akka.persistence.RecoveryCompleted
import akka.persistence.journal.EventAdapter
import reactivemongo.bson.BSONDocument
import akka.persistence.journal.EventSeq

/**
 * Quick sample project showing how the `fromJournal` method of the event adapter is never called.
 */

case class EntityEnvelope(entityId: UUID, msg: Any)
case class Location(long: Double, lat: Double)
case class Event(location: Location)

class PersistentLocationActor extends PersistentActor {
  println("Persistent actor instance constructed.")
  
  var location: Option[Location] = None
  
  override def persistenceId = s"ObservableLocation_${self.path.name}"
  
  override def receiveRecover: Receive = {
    case Event(loc) =>
      println(s"Persistent actor: Received location: $loc")
      location = Some(loc)
      
    case RecoveryCompleted =>

    case other =>
      println(s"Received weird thing: $other")
  }
  
  override def receiveCommand: Receive = {
    case newLoc: Event =>
      persist(newLoc)(receiveRecover)

    case "DIE" =>
      sender ! "OK"
      context stop self
      
    case "GET" =>
      sender ! location
  }
  
  override def postStop() {
    super.postStop()
    println("Persistent actor was shut down.")
  }
}

object Application {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("ActorSystem")
    implicit val executor = system.dispatcher
    implicit val timeout = Timeout(100, SECONDS)
    
    val region = {
      val NumberOfShards = 100

      val entityIdFromEnvelope: ShardRegion.ExtractEntityId = {
        case EntityEnvelope(entityId, msg) => (entityId.toString, msg)
      }

      val shardIdFromEnvelope: ShardRegion.ExtractShardId = {
        case EntityEnvelope(id, _) => (id.hashCode % NumberOfShards).toString
      }

      ClusterSharding(system).start(
        typeName = "PersistentLocationActor",
        entityProps = Props(new PersistentLocationActor),
        settings = ClusterShardingSettings(system),
        extractEntityId = entityIdFromEnvelope,
        extractShardId = shardIdFromEnvelope
      )
    }
    
    val address = UUID.randomUUID
    region ! EntityEnvelope(address, Event(Location(1,2)))
    await(region ? EntityEnvelope(address, "DIE"))
    Thread.sleep(1000)
    println("Asking questions from a dead persistent actor...")
    val result = await(region ? EntityEnvelope(address, "GET"))
    println(s"Result after replay: $result")
  }
  
  private def await[A](f: Future[A]): A =
    Await.result(f, 100.seconds)
}

class MyEventAdapter(system: ExtendedActorSystem) extends EventAdapter {
  override def manifest(event: Any): String = ""

  override def toJournal(event: Any): Any = {
    println("toJournal called")
    event match {
      case Event(location) =>
        BSONDocument("long" -> location.long, "lat" -> location.lat)
    }
  }

  override def fromJournal(event: Any, manifest: String): EventSeq = {
    println("fromJournal called")
    event match {
      case bson: BSONDocument =>
        val long = bson.getAs[Double]("long").get
        val lat = bson.getAs[Double]("lat").get
        EventSeq.single(Event(Location(long, lat)))
    }
  }
}
