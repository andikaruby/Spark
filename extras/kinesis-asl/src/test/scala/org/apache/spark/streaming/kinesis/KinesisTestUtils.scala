package org.apache.spark.streaming.kinesis

import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.util.Random

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.RegionUtils
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model._

import org.apache.spark.Logging

class KinesisTestUtils(val endpointUrl: String, _regionName: String = "") extends Logging {

  val regionName = if (_regionName.length == 0) {
    RegionUtils.getRegionByEndpoint(endpointUrl).getName()
  } else {
    RegionUtils.getRegion(_regionName).getName()
  }
  val streamShardCount = 2

  private val createStreamTimeoutSeconds = 300
  private val describeStreamPollTimeSeconds = 1

  @volatile
  private var streamCreated = false
  private var _streamName: String = _


  private val credentialsProvider = new DefaultAWSCredentialsProviderChain()

  private lazy val kinesisClient = {
    val client = new AmazonKinesisClient(credentialsProvider)
    client.setEndpoint(endpointUrl)
    client
  }

  private lazy val dynamoDB = {
    val dynamoDBClient = new AmazonDynamoDBClient(new DefaultAWSCredentialsProviderChain())
    dynamoDBClient.setRegion(RegionUtils.getRegion(regionName))
    new DynamoDB(dynamoDBClient)
  }

  def streamName: String = {
    require(streamCreated, "Stream not yet created, call createStream() to create one")
    _streamName
  }

  def createStream(): Unit = {
    println("Creating stream")
    require(!streamCreated, "Stream already created")
    _streamName = findNonExistentStreamName()

    // Create a stream. The number of shards determines the provisioned throughput.
    val createStreamRequest = new CreateStreamRequest()
    createStreamRequest.setStreamName(_streamName)
    createStreamRequest.setShardCount(2)
    kinesisClient.createStream(createStreamRequest)

    // The stream is now being created. Wait for it to become active.
    waitForStreamToBeActive(_streamName)
    streamCreated = true
    println("Created stream")
  }

  /**
   * Push data to Kinesis stream and return a map of
   * shardId -> seq of (data, seq number) pushed to corresponding shard
   */
  def pushData(testData: Seq[Int]): Map[String, Seq[(Int, String)]] = {
    require(streamCreated, "Stream not yet created, call createStream() to create one")
    val shardIdToSeqNumbers = new mutable.HashMap[String, ArrayBuffer[(Int, String)]]()

    testData.foreach { num =>
      val str = num.toString
      val putRecordRequest = new PutRecordRequest().withStreamName(streamName)
        .withData(ByteBuffer.wrap(str.getBytes()))
        .withPartitionKey(str)

      val putRecordResult = kinesisClient.putRecord(putRecordRequest)
      val shardId = putRecordResult.getShardId
      val seqNumber = putRecordResult.getSequenceNumber()
      val sentSeqNumbers = shardIdToSeqNumbers.getOrElseUpdate(shardId,
        new ArrayBuffer[(Int, String)]())
      sentSeqNumbers += ((num, seqNumber))

    }
    println(s"Pushed $testData:\n\t ${shardIdToSeqNumbers.mkString("\n\t")}")
    shardIdToSeqNumbers.toMap
  }

  def describeStream(streamNameToDescribe: String = streamName): Option[StreamDescription] = {
    try {
      val describeStreamRequest = new DescribeStreamRequest().withStreamName(streamNameToDescribe)
      val desc = kinesisClient.describeStream(describeStreamRequest).getStreamDescription()
      Some(desc)
    } catch {
      case rnfe: ResourceNotFoundException =>
        None
    }
  }

  def deleteStream(): Unit = {
    try {
      if (describeStream().nonEmpty) {
        val deleteStreamRequest = new DeleteStreamRequest()
        kinesisClient.deleteStream(streamName)
      }
    } catch {
      case e: Exception =>
        logWarning(s"Could not delete stream $streamName")
    }
  }

  def deleteDynamoDBTable(tableName: String): Unit = {
    try {
      val table = dynamoDB.getTable(tableName)
      table.delete()
      table.waitForDelete()
    } catch {
      case e: Exception =>
        logWarning(s"Could not delete DynamoDB table $tableName")
    }
  }

  private def findNonExistentStreamName(): String = {
    var testStreamName: String = null
    do {
      Thread.sleep(TimeUnit.SECONDS.toMillis(describeStreamPollTimeSeconds))
      testStreamName = s"KinesisTestUtils-${math.abs(Random.nextLong())}"
    } while (describeStream(testStreamName).nonEmpty)
    testStreamName
  }

  private def waitForStreamToBeActive(streamNameToWaitFor: String): Unit = {
    val startTime = System.currentTimeMillis()
    val endTime = startTime + TimeUnit.SECONDS.toMillis(createStreamTimeoutSeconds)
    while (System.currentTimeMillis() < endTime) {
      Thread.sleep(TimeUnit.SECONDS.toMillis(describeStreamPollTimeSeconds))
      describeStream(streamNameToWaitFor).foreach { description =>
        val streamStatus = description.getStreamStatus()
        logDebug(s"\t- current state: $streamStatus\n")
        if ("ACTIVE".equals(streamStatus)) {
          return
        }
      }
    }
    require(false, s"Stream $streamName never became active")
  }
}
