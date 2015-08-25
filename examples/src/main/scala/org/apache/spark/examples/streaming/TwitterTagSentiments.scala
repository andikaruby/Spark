/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// scalastyle:off println
package org.apache.spark.examples.streaming

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.streaming.twitter.TwitterUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
 * Inspired by TwitterPopularTags, this example program displays the most positive
 * hash tags by joining the streaming twitter data with a static RDD of
 * the word-sentiment file provided by http://alexdavies.net/twitter-sentiment-analysis/
 */
object TwitterTagSentiments {
  def main(args: Array[String]) {
    if (args.length < 4) {
      System.err.println("Usage: TwitterTagSentiments <consumer key> <consumer secret> " +
        "<access token> <access token secret> [<filters>]")
      System.exit(1)
    }

    StreamingExamples.setStreamingLogLevels()

    val Array(consumerKey, consumerSecret, accessToken, accessTokenSecret) = args.take(4)
    val filters = args.takeRight(args.length - 4)

    System.setProperty("twitter4j.oauth.consumerKey", consumerKey)
    System.setProperty("twitter4j.oauth.consumerSecret", consumerSecret)
    System.setProperty("twitter4j.oauth.accessToken", accessToken)
    System.setProperty("twitter4j.oauth.accessTokenSecret", accessTokenSecret)

    val sparkConf = new SparkConf().setAppName("TwitterTagSentiments")
    val ssc = new StreamingContext(sparkConf, Seconds(2))
    val stream = TwitterUtils.createStream(ssc, None, filters)

    val hashTags = stream.flatMap(status => status.getText.split(" ").filter(_.startsWith("#")))
    val wordSentimentFilePath = "examples/src/main/resources/twitter_sentiment_list.txt"
    val wordSentiments = ssc.sparkContext.textFile(wordSentimentFilePath).map { line =>
      val Array(word, happinessValue, _) = line.split(",")
      (word, happinessValue)
    }

    val happiest60 = hashTags.map(hashTag => (hashTag.tail, 1))
      .reduceByKeyAndWindow(_ + _, Seconds(60))
      .transform{topicCount => wordSentiments.join(topicCount)}
      .map{case (topic, tuple) => (topic, tuple._1 * tuple._2)}
      .map{case (topic, happinessValue) => (happinessValue, topic)}
      .transform(_.sortByKey(false))

    val happiest10 = hashTags.map(hashTag => (hashTag.tail, 1))
      .reduceByKeyAndWindow(_ + _, Seconds(10))
      .transform{topicCount => wordSentiments.join(topicCount)}
      .map{case (topic, tuple) => (topic, tuple._1 * tuple._2)}
      .map{case (topic, happinessValue) => (happinessValue, topic)}
      .transform(_.sortByKey(false))

    happiest60.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nHappiest topics in last 60 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (happiness, tag) => println("%s (%s happiness)".format(tag, happiness))}
    })

    happiest10.foreachRDD(rdd => {
      val topList = rdd.take(10)
      println("\nHappiest topics in last 10 seconds (%s total):".format(rdd.count()))
      topList.foreach{case (happiness, tag) => println("%s (%s happiness)".format(tag, happiness))}
    })

    ssc.start()
    ssc.awaitTermination()
  }
}
// scalastyle:on println
