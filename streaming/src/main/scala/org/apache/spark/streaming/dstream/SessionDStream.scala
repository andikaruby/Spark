package org.apache.spark.streaming.dstream

import java.io.{IOException, ObjectOutputStream}

import scala.collection.mutable
import scala.reflect.ClassTag

import org.apache.spark._
import org.apache.spark.rdd.{EmptyRDD, RDD}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Duration, Time}
import org.apache.spark.util.Utils


// ==================================================
// ==================================================
// ================= PUBLIC CLASSES =================
// ==================================================
// ==================================================


/** Represents a session */
case class Session[K, S] private[streaming](
  private var key: K, private var data: S, private var active: Boolean) {

  def this() = this(null.asInstanceOf[K], null.asInstanceOf[S], true)

  private[streaming] def set(k: K, s: S, a: Boolean): this.type = {
    key = k
    data = s
    active = a
    this
  }

  /** Get the session key */
  def getKey(): K = key

  /** Get the session value */
  def getData(): S = data

  /** Whether the session is active */
  def isActive(): Boolean = active
/*
  override def toString(): String = {
    s"Session[ Key=$key, Data=$session, active=$active ]"
  }*/
}

private[streaming] object Session {

}

/** Class representing all the specification of session */
class SessionSpec[K: ClassTag, V: ClassTag, S: ClassTag] private[streaming]() extends Serializable {
  @volatile private var updateFunction: (V, Option[S]) => Option[S] = null
  @volatile private var partitioner: Partitioner = null
  @volatile private var initialSessionRDD: RDD[(K, S)] = null
  @volatile private var allSessions: Boolean = false

  def setPartition(partitioner: Partitioner): this.type = {
    this.partitioner = partitioner
    this
  }

  def setUpdateFunction(func: (V, Option[S]) => Option[S]): this.type = {
    updateFunction = func
    this
  }

  def setInitialSessions(initialRDD: RDD[(K, S)]): this.type = {
    this.initialSessionRDD = initialRDD
    this
  }

  def reportAllSession(allSessions: Boolean): this.type = {
    this.allSessions = allSessions
    this
  }

  private[streaming] def getPartitioner(): Option[Partitioner] = Option(partitioner)

  private[streaming] def getUpdateFunction(): (V, Option[S]) => Option[S] = updateFunction

  private[streaming] def getInitialSessions(): Option[RDD[(K, S)]] = Option(initialSessionRDD)

  private[streaming] def getAllSessions(): Boolean = allSessions

  private[streaming] def validate(): Unit = {
    require(updateFunction != null)
  }
}

object SessionSpec {
  def create[K: ClassTag, V: ClassTag, S: ClassTag](
                                                     updateFunction: (V, Option[S]) => Option[S]): SessionSpec[K, V, S] = {
    new SessionSpec[K, V, S].setUpdateFunction(updateFunction)
  }
}



// ===============================================
// ===============================================
// ============== PRIVATE CLASSES ================
// ===============================================
// ===============================================



// -----------------------------------------------
// --------------- SessionStore stuff --------------
// -----------------------------------------------

/**
 * Internal interface for defining the map that keeps track of sessions.
 */
private[streaming] abstract class SessionStore[K: ClassTag, S: ClassTag] extends Serializable {
  /** Add or update session data */

  def put(key: K, session: S): Unit

  /** Get the session data if it exists */
  def get(key: K): Option[S]

  /** Remove a key */
  def remove(key: K): Unit

  /**
   * Shallow copy the map to create a new session store. Updates to the new map
   * should not mutate `this` map.
   */
  def copy(): SessionStore[K, S]

  /**
   * Return an iterator of data in this map. If th flag is true, implementations should
   * return only the session that were updated since the creation of this map.
   */
  def iterator(updatedSessionsOnly: Boolean): Iterator[Session[K, S]]
}

private[streaming] object SessionStore {
  def empty[K: ClassTag, S: ClassTag]: SessionStore[K, S] = new EmptySessionStore[K, S]

  def create[K: ClassTag, S: ClassTag](): SessionStore[K, S] = new HashMapBasedSessionStore[K, S]()
}

/** Specific implementation of SessionStore interface representing an empty map */
private[streaming] class EmptySessionStore[K: ClassTag, S: ClassTag] extends SessionStore[K, S] {
  override def put(key: K, session: S): Unit = ???
  override def get(key: K): Option[S] = None
  override def copy(): SessionStore[K, S] = new EmptySessionStore[K, S]
  override def remove(key: K): Unit = { }
  override def iterator(updatedSessionsOnly: Boolean): Iterator[Session[K, S]] = Iterator.empty
}


/** Specific implementation of the SessionMap interface using a scala mutable HashMap */
private[streaming] class HashMapBasedSessionStore[K: ClassTag, S: ClassTag](
    parentSessionStore: SessionStore[K, S]) extends SessionStore[K, S] {

  def this() = this(new EmptySessionStore[K, S])

  import HashMapBasedSessionStore._

  private val generation: Int = parentSessionStore match {
    case map: HashMapBasedSessionStore[_, _] => map.generation + 1
    case _ => 1
  }

  private val internalMap = new mutable.HashMap[K, SessionInfo[S]]

  override def put(key: K, session: S): Unit = {
    internalMap.get(key) match {
      case Some(sessionInfo) =>
        sessionInfo.data = session
      case None =>
        internalMap.put(key, new SessionInfo(session))
    }
  }

  /** Get the session data if it exists */
  override def get(key: K): Option[S] = {
    internalMap.get(key).filter { _.deleted == false }.map { _.data }.orElse(parentSessionStore.get(key))
  }

  /** Remove a key */
  override def remove(key: K): Unit = {
    internalMap.put(key, new SessionInfo(get(key).getOrElse(null.asInstanceOf[S]), deleted = true))
  }

  /**
   * Return an iterator of data in this map. If th flag is true, implementations should
   * return only the session that were updated since the creation of this map.
   */
  override def iterator(updatedSessionsOnly: Boolean): Iterator[Session[K, S]] = {
    val updatedSessions = internalMap.iterator.map { case (key, sessionInfo) =>
      Session(key, sessionInfo.data, !sessionInfo.deleted)
    }

    def previousSessions = parentSessionStore.iterator(updatedSessionsOnly = false).filter { session =>
      !internalMap.contains(session.getKey())
    }

    if (updatedSessionsOnly) {
      updatedSessions
    } else {
      previousSessions ++ updatedSessions
    }
  }

  /**
   * Shallow copy the map to create a new session store. Updates to the new map
   * should not mutate `this` map.
   */
  override def copy(): SessionStore[K, S] = {
    doCopy(generation >= HashMapBasedSessionStore.GENERATION_THRESHOLD_FOR_CONSOLIDATION)
  }

  def doCopy(consolidate: Boolean): SessionStore[K, S] = {
    if (consolidate) {
      val newParentMap = new HashMapBasedSessionStore[K, S]()
      iterator(updatedSessionsOnly = false).filter { _.isActive }.foreach { case session =>
        newParentMap.internalMap.put(session.getKey(), SessionInfo(session.getData(), deleted = false))
      }
      new HashMapBasedSessionStore[K, S](newParentMap)
    } else {
      new HashMapBasedSessionStore[K, S](this)
    }
  }
}

private[streaming] object HashMapBasedSessionStore {

  case class SessionInfo[SessionDataType](var data: SessionDataType, var deleted: Boolean = false)

  val GENERATION_THRESHOLD_FOR_CONSOLIDATION = 10
}


// -----------------------------------------------
// --------------- SessionRDD stuff --------------
// -----------------------------------------------

private[streaming] class SessionRDDPartition(
    idx: Int,
    @transient private var previousSessionRDD: RDD[_],
    @transient private var partitionedDataRDD: RDD[_]) extends Partition {

  private[dstream] var previousSessionRDDPartition: Partition = null
  private[dstream] var partitionedDataRDDPartition: Partition = null

  override def index: Int = idx
  override def hashCode(): Int = idx

  @throws(classOf[IOException])
  private def writeObject(oos: ObjectOutputStream): Unit = Utils.tryOrIOException {
    // Update the reference to parent split at the time of task serialization
    previousSessionRDDPartition = previousSessionRDD.partitions(index)
    partitionedDataRDDPartition = partitionedDataRDD.partitions(index)
    oos.defaultWriteObject()
  }
}

private[streaming] class SessionRDD[K: ClassTag, V: ClassTag, S: ClassTag](
    _sc: SparkContext,
    private var previousSessionRDD: RDD[SessionStore[K, S]],
    private var partitionedDataRDD: RDD[(K, V)],
    updateFunction: (V, Option[S]) => Option[S],
    timestamp: Long
  ) extends RDD[SessionStore[K, S]](
    _sc,
    List(new OneToOneDependency(previousSessionRDD), new OneToOneDependency(partitionedDataRDD))
  ) {

  require(partitionedDataRDD.partitioner == previousSessionRDD.partitioner)

  override val partitioner = previousSessionRDD.partitioner

  override def compute(partition: Partition, context: TaskContext): Iterator[SessionStore[K, S]] = {
    val sessionRDDPartition = partition.asInstanceOf[SessionRDDPartition]
    val prevSessionIterator = previousSessionRDD.iterator(
      sessionRDDPartition.previousSessionRDDPartition, context)
    val dataIterator = partitionedDataRDD.iterator(
      sessionRDDPartition.partitionedDataRDDPartition, context)

    require(prevSessionIterator.hasNext)

    val sessionStore = prevSessionIterator.next().copy()
    dataIterator.foreach { case (key, value) =>
      val prevState = sessionStore.get(key)
      val newState = updateFunction(value, prevState)
      if (newState.isDefined) {
        sessionStore.put(key, newState.get)
      } else {
        sessionStore.remove(key)
      }
    }
    Iterator(sessionStore)
  }

  override protected def getPartitions: Array[Partition] = {
    Array.tabulate(previousSessionRDD.partitions.length) { i =>
      new SessionRDDPartition(i, previousSessionRDD, partitionedDataRDD)}
  }

  override def clearDependencies() {
    super.clearDependencies()
    previousSessionRDD = null
    partitionedDataRDD = null
  }
}

private[streaming] object SessionRDD {
  def createFromPairRDD[K: ClassTag, S: ClassTag](
      pairRDD: RDD[(K, S)], partitioner: Partitioner): RDD[SessionStore[K, S]] = {

    val createStateMap = (iterator: Iterator[(K, S)]) => {
      val newSessionStore = SessionStore.create[K, S]()
      iterator.foreach { case (key, state) => newSessionStore.put(key, state) }
      Iterator(newSessionStore)
    }
    pairRDD.partitionBy(partitioner).mapPartitions[SessionStore[K, S]](
      createStateMap, preservesPartitioning = true)
  }
}


// -----------------------------------------------
// ---------------- SessionDStream ---------------
// -----------------------------------------------


private[streaming] class SessionDStream[K: ClassTag, V: ClassTag, S: ClassTag](
    parent: DStream[(K, V)], sessionSpec: SessionSpec[K, V, S])
  extends DStream[SessionStore[K, S]](parent.context) {

  sessionSpec.validate()
  persist(StorageLevel.DISK_ONLY)

  private val partitioner = sessionSpec.getPartitioner().getOrElse(
    new HashPartitioner(ssc.sc.defaultParallelism))

  private val updateFunction = sessionSpec.getUpdateFunction()

  override def slideDuration: Duration = parent.slideDuration

  override def dependencies: List[DStream[_]] = List(parent)

  override val mustCheckpoint = true

  /** Method that generates a RDD for the given time */
  override def compute(validTime: Time): Option[RDD[SessionStore[K, S]]] = {
    val previousSessionRDD = getOrCompute(validTime - slideDuration).getOrElse {
      SessionRDD.createFromPairRDD[K, S](
        sessionSpec.getInitialSessions().getOrElse(new EmptyRDD[(K, S)](ssc.sparkContext)),
        partitioner
      )
    }
    val newDataRDD = parent.getOrCompute(validTime).get
    val partitionedDataRDD = newDataRDD.partitionBy(partitioner)
    Some(new SessionRDD(
      ssc.sparkContext, previousSessionRDD, partitionedDataRDD,
      updateFunction, validTime.milliseconds))
  }
}
