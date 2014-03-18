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

package org.apache.spark.mllib.recommendation

import scala.collection.mutable.{ArrayBuffer, BitSet}
import scala.math.{abs, sqrt}
import scala.util.Random
import scala.util.Sorting

import org.apache.spark.broadcast.Broadcast
import org.apache.spark.{Logging, HashPartitioner, Partitioner, SparkContext, SparkConf}
import org.apache.spark.storage.StorageLevel
import org.apache.spark.rdd.RDD
import org.apache.spark.serializer.KryoRegistrator
import org.apache.spark.SparkContext._

import com.esotericsoftware.kryo.Kryo
import org.jblas.{DoubleMatrix, SimpleBlas, Solve}


/**
 * Out-link information for a user or product block. This includes the original user/product IDs
 * of the elements within this block, and the list of destination blocks that each user or
 * product will need to send its feature vector to.
 */
private[recommendation] case class OutLinkBlock(elementIds: Array[Int], shouldSend: Array[BitSet])


/**
 * In-link information for a user (or product) block. This includes the original user/product IDs
 * of the elements within this block, as well as an array of indices and ratings that specify
 * which user in the block will be rated by which products from each product block (or vice-versa).
 * Specifically, if this InLinkBlock is for users, ratingsForBlock(b)(i) will contain two arrays,
 * indices and ratings, for the i'th product that will be sent to us by product block b (call this
 * P). These arrays represent the users that product P had ratings for (by their index in this
 * block), as well as the corresponding rating for each one. We can thus use this information when
 * we get product block b's message to update the corresponding users.
 */
private[recommendation] case class InLinkBlock(
  elementIds: Array[Int], ratingsForBlock: Array[Array[(Array[Int], Array[Double])]])


/**
 * A more compact class to represent a rating than Tuple3[Int, Int, Double].
 */
case class Rating(val user: Int, val product: Int, val rating: Double)

/**
 * Alternating Least Squares matrix factorization.
 *
 * ALS attempts to estimate the ratings matrix `R` as the product of two lower-rank matrices,
 * `X` and `Y`, i.e. `X * Yt = R`. Typically these approximations are called 'factor' matrices.
 * The general approach is iterative. During each iteration, one of the factor matrices is held
 * constant, while the other is solved for using least squares. The newly-solved factor matrix is
 * then held constant while solving for the other factor matrix.
 *
 * This is a blocked implementation of the ALS factorization algorithm that groups the two sets
 * of factors (referred to as "users" and "products") into blocks and reduces communication by only
 * sending one copy of each user vector to each product block on each iteration, and only for the
 * product blocks that need that user's feature vector. This is achieved by precomputing some
 * information about the ratings matrix to determine the "out-links" of each user (which blocks of
 * products it will contribute to) and "in-link" information for each product (which of the feature
 * vectors it receives from each user block it will depend on). This allows us to send only an
 * array of feature vectors between each user block and product block, and have the product block
 * find the users' ratings and update the products based on these messages.
 *
 * For implicit preference data, the algorithm used is based on
 * "Collaborative Filtering for Implicit Feedback Datasets", available at
 * [[http://dx.doi.org/10.1109/ICDM.2008.22]], adapted for the blocked approach used here.
 *
 * Essentially instead of finding the low-rank approximations to the rating matrix `R`,
 * this finds the approximations for a preference matrix `P` where the elements of `P` are 1 if
 * r > 0 and 0 if r = 0. The ratings then act as 'confidence' values related to strength of
 * indicated user
 * preferences rather than explicit ratings given to items.
 */
class ALS private (
    var numBlocks: Int,
    var rank: Int,
    var iterations: Int,
    var lambda: Double,
    var implicitPrefs: Boolean,
    var alpha: Double,
    var seed: Long = System.nanoTime()
  ) extends Serializable with Logging {
  def this() = this(-1, 10, 10, 0.01, false, 1.0)

  /**
   * Set the number of blocks to parallelize the computation into; pass -1 for an auto-configured
   * number of blocks. Default: -1.
   */
  def setBlocks(numBlocks: Int): ALS = {
    this.numBlocks = numBlocks
    this
  }

  /** Set the rank of the feature matrices computed (number of features). Default: 10. */
  def setRank(rank: Int): ALS = {
    this.rank = rank
    this
  }

  /** Set the number of iterations to run. Default: 10. */
  def setIterations(iterations: Int): ALS = {
    this.iterations = iterations
    this
  }

  /** Set the regularization parameter, lambda. Default: 0.01. */
  def setLambda(lambda: Double): ALS = {
    this.lambda = lambda
    this
  }

  def setImplicitPrefs(implicitPrefs: Boolean): ALS = {
    this.implicitPrefs = implicitPrefs
    this
  }

  def setAlpha(alpha: Double): ALS = {
    this.alpha = alpha
    this
  }

  /** Sets a random seed to have deterministic results. */
  def setSeed(seed: Long): ALS = {
    this.seed = seed
    this
  }

  /**
   * Run ALS with the configured parameters on an input RDD of (user, product, rating) triples.
   * Returns a MatrixFactorizationModel with feature vectors for each user and product.
   */
  def run(ratings: RDD[Rating]): MatrixFactorizationModel = {
    val sc = ratings.context

    val numBlocks = if (this.numBlocks == -1) {
      math.max(sc.defaultParallelism, ratings.partitions.size / 2)
    } else {
      this.numBlocks
    }

    val partitioner = new HashPartitioner(numBlocks)

    val ratingsByUserBlock = ratings.map{ rating => (rating.user % numBlocks, rating) }
    val ratingsByProductBlock = ratings.map{ rating =>
      (rating.product % numBlocks, Rating(rating.product, rating.user, rating.rating))
    }

    val (userInLinks, userOutLinks) = makeLinkRDDs(numBlocks, ratingsByUserBlock)
    val (productInLinks, productOutLinks) = makeLinkRDDs(numBlocks, ratingsByProductBlock)

    // Initialize user and product factors randomly, but use a deterministic seed for each
    // partition so that fault recovery works
    val seedGen = new Random(seed)
    val seed1 = seedGen.nextInt()
    val seed2 = seedGen.nextInt()
    // Hash an integer to propagate random bits at all positions, similar to java.util.HashTable
    def hash(x: Int): Int = {
      val r = x ^ (x >>> 20) ^ (x >>> 12)
      r ^ (r >>> 7) ^ (r >>> 4)
    }
    var users = userOutLinks.mapPartitionsWithIndex { (index, itr) =>
      val rand = new Random(hash(seed1 ^ index))
      itr.map { case (x, y) =>
        (x, y.elementIds.map(_ => randomFactor(rank, rand)))
      }
    }
    var products = productOutLinks.mapPartitionsWithIndex { (index, itr) =>
      val rand = new Random(hash(seed2 ^ index))
      itr.map { case (x, y) =>
        (x, y.elementIds.map(_ => randomFactor(rank, rand)))
      }
    }

    if (implicitPrefs) {
      for (iter <- 1 to iterations) {
        // perform ALS update
        logInfo("Re-computing I given U (Iteration %d/%d)".format(iter, iterations))
        // Persist users because it will be called twice.
        users.persist()
        val YtY = Some(sc.broadcast(computeYtY(users)))
        val previousProducts = products
        products = updateFeatures(users, userOutLinks, productInLinks, partitioner, rank, lambda,
          alpha, YtY)
        previousProducts.unpersist()
        logInfo("Re-computing U given I (Iteration %d/%d)".format(iter, iterations))
        products.persist()
        val XtX = Some(sc.broadcast(computeYtY(products)))
        val previousUsers = users
        users = updateFeatures(products, productOutLinks, userInLinks, partitioner, rank, lambda,
          alpha, XtX)
        previousUsers.unpersist()
      }
    } else {
      for (iter <- 1 to iterations) {
        // perform ALS update
        logInfo("Re-computing I given U (Iteration %d/%d)".format(iter, iterations))
        products = updateFeatures(users, userOutLinks, productInLinks, partitioner, rank, lambda,
          alpha, YtY = None)
        logInfo("Re-computing U given I (Iteration %d/%d)".format(iter, iterations))
        users = updateFeatures(products, productOutLinks, userInLinks, partitioner, rank, lambda,
          alpha, YtY = None)
      }
    }

    products.persist()

    // Flatten and cache the two final RDDs to un-block them
    val usersOut = unblockFactors(users, userOutLinks)
    val productsOut = unblockFactors(products, productOutLinks)

    usersOut.persist()
    productsOut.persist()

    // Materialize usersOut and productsOut.
    usersOut.count()
    productsOut.count()

    products.unpersist()

    // Clean up.
    userInLinks.unpersist()
    userOutLinks.unpersist()
    productInLinks.unpersist()
    productOutLinks.unpersist()

    new MatrixFactorizationModel(rank, usersOut, productsOut)
  }

  /**
   * Computes the (`rank x rank`) matrix `YtY`, where `Y` is the (`nui x rank`) matrix of factors
   * for each user (or product), in a distributed fashion.
   *
   * @param factors the (block-distributed) user or product factor vectors
   * @return YtY - whose value is only used in the implicit preference model
   */
  private def computeYtY(factors: RDD[(Int, Array[Array[Double]])]) = {
    val n = rank * (rank + 1) / 2
    val LYtY = factors.values.aggregate(new DoubleMatrix(n))( seqOp = (L, Y) => {
      Y.foreach(y => dspr(1.0, new DoubleMatrix(y), L))
      L
    }, combOp = (L1, L2) => {
      L1.addi(L2)
    })
    val YtY = new DoubleMatrix(rank, rank)
    fillFullMatrix(LYtY, YtY)
    YtY
  }

  /**
   * Adds alpha * x * x.t to a matrix in-place. This is the same as BLAS's DSPR.
   *
   * @param L the lower triangular part of the matrix packed in an array (row major)
   */
  private def dspr(alpha: Double, x: DoubleMatrix, L: DoubleMatrix) = {
    val n = x.length
    var i = 0
    var j = 0
    var idx = 0
    var axi = 0.0
    val xd = x.data
    val Ld = L.data
    while (i < n) {
      axi = alpha * xd(i)
      j = 0
      while (j <= i) {
        Ld(idx) += axi * xd(j)
        j += 1
        idx += 1
      }
      i += 1
    }
  }

  /**
   * Flatten out blocked user or product factors into an RDD of (id, factor vector) pairs
   */
  private def unblockFactors(blockedFactors: RDD[(Int, Array[Array[Double]])],
                     outLinks: RDD[(Int, OutLinkBlock)]) = {
    blockedFactors.join(outLinks).flatMap{ case (b, (factors, outLinkBlock)) =>
      for (i <- 0 until factors.length) yield (outLinkBlock.elementIds(i), factors(i))
    }
  }

  /**
   * Make the out-links table for a block of the users (or products) dataset given the list of
   * (user, product, rating) values for the users in that block (or the opposite for products).
   */
  private def makeOutLinkBlock(numBlocks: Int, ratings: Array[Rating]): OutLinkBlock = {
    val userIds = ratings.map(_.user).distinct.sorted
    val numUsers = userIds.length
    val userIdToPos = userIds.zipWithIndex.toMap
    val shouldSend = Array.fill(numUsers)(new BitSet(numBlocks))
    for (r <- ratings) {
      shouldSend(userIdToPos(r.user))(r.product % numBlocks) = true
    }
    OutLinkBlock(userIds, shouldSend)
  }

  /**
   * Make the in-links table for a block of the users (or products) dataset given a list of
   * (user, product, rating) values for the users in that block (or the opposite for products).
   */
  private def makeInLinkBlock(numBlocks: Int, ratings: Array[Rating]): InLinkBlock = {
    val userIds = ratings.map(_.user).distinct.sorted
    val numUsers = userIds.length
    val userIdToPos = userIds.zipWithIndex.toMap
    // Split out our ratings by product block
    val blockRatings = Array.fill(numBlocks)(new ArrayBuffer[Rating])
    for (r <- ratings) {
      blockRatings(r.product % numBlocks) += r
    }
    val ratingsForBlock = new Array[Array[(Array[Int], Array[Double])]](numBlocks)
    for (productBlock <- 0 until numBlocks) {
      // Create an array of (product, Seq(Rating)) ratings
      val groupedRatings = blockRatings(productBlock).groupBy(_.product).toArray
      // Sort them by product ID
      val ordering = new Ordering[(Int, ArrayBuffer[Rating])] {
        def compare(a: (Int, ArrayBuffer[Rating]), b: (Int, ArrayBuffer[Rating])): Int =
            a._1 - b._1
      }
      Sorting.quickSort(groupedRatings)(ordering)
      // Translate the user IDs to indices based on userIdToPos
      ratingsForBlock(productBlock) = groupedRatings.map { case (p, rs) =>
        (rs.view.map(r => userIdToPos(r.user)).toArray, rs.view.map(_.rating).toArray)
      }
    }
    InLinkBlock(userIds, ratingsForBlock)
  }

  /**
   * Make RDDs of InLinkBlocks and OutLinkBlocks given an RDD of (blockId, (u, p, r)) values for
   * the users (or (blockId, (p, u, r)) for the products). We create these simultaneously to avoid
   * having to shuffle the (blockId, (u, p, r)) RDD twice, or to cache it.
   */
  private def makeLinkRDDs(numBlocks: Int, ratings: RDD[(Int, Rating)])
    : (RDD[(Int, InLinkBlock)], RDD[(Int, OutLinkBlock)]) =
  {
    val grouped = ratings.partitionBy(new HashPartitioner(numBlocks))
    val links = grouped.mapPartitionsWithIndex((blockId, elements) => {
      val ratings = elements.map{_._2}.toArray
      val inLinkBlock = makeInLinkBlock(numBlocks, ratings)
      val outLinkBlock = makeOutLinkBlock(numBlocks, ratings)
      Iterator.single((blockId, (inLinkBlock, outLinkBlock)))
    }, true)
    val inLinks = links.mapValues(_._1)
    val outLinks = links.mapValues(_._2)
    inLinks.persist(StorageLevel.MEMORY_AND_DISK)
    outLinks.persist(StorageLevel.MEMORY_AND_DISK)
    (inLinks, outLinks)
  }

  /**
   * Make a random factor vector with the given random.
   */
  private def randomFactor(rank: Int, rand: Random): Array[Double] = {
    // Choose a unit vector uniformly at random from the unit sphere, but from the
    // "first quadrant" where all elements are nonnegative. This can be done by choosing
    // elements distributed as Normal(0,1) and taking the absolute value, and then normalizing.
    // This appears to create factorizations that have a slightly better reconstruction
    // (<1%) compared picking elements uniformly at random in [0,1].
    val factor = Array.fill(rank)(abs(rand.nextGaussian()))
    val norm = sqrt(factor.map(x => x * x).sum)
    factor.map(x => x / norm)
  }

  /**
   * Compute the user feature vectors given the current products (or vice-versa). This first joins
   * the products with their out-links to generate a set of messages to each destination block
   * (specifically, the features for the products that user block cares about), then groups these
   * by destination and joins them with the in-link info to figure out how to update each user.
   * It returns an RDD of new feature vectors for each user block.
   */
  private def updateFeatures(
      products: RDD[(Int, Array[Array[Double]])],
      productOutLinks: RDD[(Int, OutLinkBlock)],
      userInLinks: RDD[(Int, InLinkBlock)],
      partitioner: Partitioner,
      rank: Int,
      lambda: Double,
      alpha: Double,
      YtY: Option[Broadcast[DoubleMatrix]])
    : RDD[(Int, Array[Array[Double]])] =
  {
    val numBlocks = products.partitions.size
    productOutLinks.join(products).flatMap { case (bid, (outLinkBlock, factors)) =>
        val toSend = Array.fill(numBlocks)(new ArrayBuffer[Array[Double]])
        for (p <- 0 until outLinkBlock.elementIds.length; userBlock <- 0 until numBlocks) {
          if (outLinkBlock.shouldSend(p)(userBlock)) {
            toSend(userBlock) += factors(p)
          }
        }
        toSend.zipWithIndex.map{ case (buf, idx) => (idx, (bid, buf.toArray)) }
    }.groupByKey(partitioner)
     .join(userInLinks)
     .mapValues{ case (messages, inLinkBlock) =>
        updateBlock(messages, inLinkBlock, rank, lambda, alpha, YtY)
      }
  }

  /**
   * Compute the new feature vectors for a block of the users matrix given the list of factors
   * it received from each product and its InLinkBlock.
   */
  private def updateBlock(messages: Seq[(Int, Array[Array[Double]])], inLinkBlock: InLinkBlock,
      rank: Int, lambda: Double, alpha: Double, YtY: Option[Broadcast[DoubleMatrix]])
    : Array[Array[Double]] =
  {
    // Sort the incoming block factor messages by block ID and make them an array
    val blockFactors = messages.sortBy(_._1).map(_._2).toArray // Array[Array[Double]]
    val numBlocks = blockFactors.length
    val numUsers = inLinkBlock.elementIds.length

    // We'll sum up the XtXes using vectors that represent only the lower-triangular part, since
    // the matrices are symmetric
    val triangleSize = rank * (rank + 1) / 2
    val userXtX = Array.fill(numUsers)(DoubleMatrix.zeros(triangleSize))
    val userXy = Array.fill(numUsers)(DoubleMatrix.zeros(rank))

    // Some temp variables to avoid memory allocation
    val tempXtX = DoubleMatrix.zeros(triangleSize)
    val fullXtX = DoubleMatrix.zeros(rank, rank)

    // Compute the XtX and Xy values for each user by adding products it rated in each product
    // block
    for (productBlock <- 0 until numBlocks) {
      for (p <- 0 until blockFactors(productBlock).length) {
        val x = new DoubleMatrix(blockFactors(productBlock)(p))
        tempXtX.fill(0.0)
        dspr(1.0, x, tempXtX)
        val (us, rs) = inLinkBlock.ratingsForBlock(productBlock)(p)
        for (i <- 0 until us.length) {
          if (implicitPrefs) {
            // Extension to the original paper to handle rs(i) < 0. confidence is a function
            // of |rs(i)| instead so that it is never negative:
            val confidence = 1 + alpha * abs(rs(i))
            SimpleBlas.axpy(confidence - 1.0, tempXtX, userXtX(us(i)))
            // For rs(i) < 0, the corresponding entry in P is 0 now, not 1 -- negative rs(i)
            // means we try to reconstruct 0. We add terms only where P = 1, so, term below
            // is now only added for rs(i) > 0:
            if (rs(i) > 0) {
              SimpleBlas.axpy(confidence, x, userXy(us(i)))
            }
          } else {
            userXtX(us(i)).addi(tempXtX)
            SimpleBlas.axpy(rs(i), x, userXy(us(i)))
          }
        }
      }
    }

    // Solve the least-squares problem for each user and return the new feature vectors
    Array.range(0, numUsers).map { index =>
      // Compute the full XtX matrix from the lower-triangular part we got above
      fillFullMatrix(userXtX(index), fullXtX)
      // Add regularization
      (0 until rank).foreach(i => fullXtX.data(i*rank + i) += lambda)
      // Solve the resulting matrix, which is symmetric and positive-definite
      if (implicitPrefs) {
        Solve.solvePositive(fullXtX.addi(YtY.get.value), userXy(index)).data
      } else {
        Solve.solvePositive(fullXtX, userXy(index)).data
      }
    }
  }

  /**
   * Given a triangular matrix in the order of fillXtX above, compute the full symmetric square
   * matrix that it represents, storing it into destMatrix.
   */
  private def fillFullMatrix(triangularMatrix: DoubleMatrix, destMatrix: DoubleMatrix) {
    val rank = destMatrix.rows
    var i = 0
    var pos = 0
    while (i < rank) {
      var j = 0
      while (j <= i) {
        destMatrix.data(i*rank + j) = triangularMatrix.data(pos)
        destMatrix.data(j*rank + i) = triangularMatrix.data(pos)
        pos += 1
        j += 1
      }
      i += 1
    }
  }
}


/**
 * Top-level methods for calling Alternating Least Squares (ALS) matrix factorization.
 */
object ALS {

  /**
   * Train a matrix factorization model given an RDD of ratings given by users to some products,
   * in the form of (userID, productID, rating) pairs. We approximate the ratings matrix as the
   * product of two lower-rank matrices of a given rank (number of features). To solve for these
   * features, we run a given number of iterations of ALS. This is done using a level of
   * parallelism given by `blocks`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   * @param blocks     level of parallelism to split computation into
   * @param seed       random seed
   */
  def train(
      ratings: RDD[Rating],
      rank: Int,
      iterations: Int,
      lambda: Double,
      blocks: Int,
      seed: Long
    ): MatrixFactorizationModel = {
    new ALS(blocks, rank, iterations, lambda, false, 1.0, seed).run(ratings)
  }

  /**
   * Train a matrix factorization model given an RDD of ratings given by users to some products,
   * in the form of (userID, productID, rating) pairs. We approximate the ratings matrix as the
   * product of two lower-rank matrices of a given rank (number of features). To solve for these
   * features, we run a given number of iterations of ALS. This is done using a level of
   * parallelism given by `blocks`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   * @param blocks     level of parallelism to split computation into
   */
  def train(
      ratings: RDD[Rating],
      rank: Int,
      iterations: Int,
      lambda: Double,
      blocks: Int
    ): MatrixFactorizationModel = {
    new ALS(blocks, rank, iterations, lambda, false, 1.0).run(ratings)
  }

  /**
   * Train a matrix factorization model given an RDD of ratings given by users to some products,
   * in the form of (userID, productID, rating) pairs. We approximate the ratings matrix as the
   * product of two lower-rank matrices of a given rank (number of features). To solve for these
   * features, we run a given number of iterations of ALS. The level of parallelism is determined
   * automatically based on the number of partitions in `ratings`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   */
  def train(ratings: RDD[Rating], rank: Int, iterations: Int, lambda: Double)
    : MatrixFactorizationModel = {
    train(ratings, rank, iterations, lambda, -1)
  }

  /**
   * Train a matrix factorization model given an RDD of ratings given by users to some products,
   * in the form of (userID, productID, rating) pairs. We approximate the ratings matrix as the
   * product of two lower-rank matrices of a given rank (number of features). To solve for these
   * features, we run a given number of iterations of ALS. The level of parallelism is determined
   * automatically based on the number of partitions in `ratings`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   */
  def train(ratings: RDD[Rating], rank: Int, iterations: Int)
    : MatrixFactorizationModel = {
    train(ratings, rank, iterations, 0.01, -1)
  }

  /**
   * Train a matrix factorization model given an RDD of 'implicit preferences' given by users
   * to some products, in the form of (userID, productID, preference) pairs. We approximate the
   * ratings matrix as the product of two lower-rank matrices of a given rank (number of features).
   * To solve for these features, we run a given number of iterations of ALS. This is done using
   * a level of parallelism given by `blocks`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   * @param blocks     level of parallelism to split computation into
   * @param alpha      confidence parameter (only applies when immplicitPrefs = true)
   * @param seed       random seed
   */
  def trainImplicit(
      ratings: RDD[Rating],
      rank: Int,
      iterations: Int,
      lambda: Double,
      blocks: Int,
      alpha: Double,
      seed: Long
    ): MatrixFactorizationModel = {
    new ALS(blocks, rank, iterations, lambda, true, alpha, seed).run(ratings)
  }

  /**
   * Train a matrix factorization model given an RDD of 'implicit preferences' given by users
   * to some products, in the form of (userID, productID, preference) pairs. We approximate the
   * ratings matrix as the product of two lower-rank matrices of a given rank (number of features).
   * To solve for these features, we run a given number of iterations of ALS. This is done using
   * a level of parallelism given by `blocks`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   * @param blocks     level of parallelism to split computation into
   * @param alpha      confidence parameter (only applies when immplicitPrefs = true)
   */
  def trainImplicit(
      ratings: RDD[Rating],
      rank: Int,
      iterations: Int,
      lambda: Double,
      blocks: Int,
      alpha: Double
    ): MatrixFactorizationModel = {
    new ALS(blocks, rank, iterations, lambda, true, alpha).run(ratings)
  }

  /**
   * Train a matrix factorization model given an RDD of 'implicit preferences' given by users to
   * some products, in the form of (userID, productID, preference) pairs. We approximate the
   * ratings matrix as the product of two lower-rank matrices of a given rank (number of features).
   * To solve for these features, we run a given number of iterations of ALS. The level of
   * parallelism is determined automatically based on the number of partitions in `ratings`.
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   * @param lambda     regularization factor (recommended: 0.01)
   */
  def trainImplicit(ratings: RDD[Rating], rank: Int, iterations: Int, lambda: Double, alpha: Double)
    : MatrixFactorizationModel = {
    trainImplicit(ratings, rank, iterations, lambda, -1, alpha)
  }

  /**
   * Train a matrix factorization model given an RDD of 'implicit preferences' ratings given by
   * users to some products, in the form of (userID, productID, rating) pairs. We approximate the
   * ratings matrix as the product of two lower-rank matrices of a given rank (number of features).
   * To solve for these features, we run a given number of iterations of ALS. The level of
   * parallelism is determined automatically based on the number of partitions in `ratings`.
   * Model parameters `alpha` and `lambda` are set to reasonable default values
   *
   * @param ratings    RDD of (userID, productID, rating) pairs
   * @param rank       number of features to use
   * @param iterations number of iterations of ALS (recommended: 10-20)
   */
  def trainImplicit(ratings: RDD[Rating], rank: Int, iterations: Int)
    : MatrixFactorizationModel = {
    trainImplicit(ratings, rank, iterations, 0.01, -1, 1.0)
  }

  private class ALSRegistrator extends KryoRegistrator {
    override def registerClasses(kryo: Kryo) {
      kryo.register(classOf[Rating])
    }
  }

  def main(args: Array[String]) {
    if (args.length < 5 || args.length > 9) {
      println("Usage: ALS <master> <ratings_file> <rank> <iterations> <output_dir> " +
        "[<lambda>] [<implicitPrefs>] [<alpha>] [<blocks>]")
      System.exit(1)
    }
    val (master, ratingsFile, rank, iters, outputDir) =
      (args(0), args(1), args(2).toInt, args(3).toInt, args(4))
    val lambda = if (args.length >= 6) args(5).toDouble else 0.01
    val implicitPrefs = if (args.length >= 7) args(6).toBoolean else false
    val alpha = if (args.length >= 8) args(7).toDouble else 1
    val blocks = if (args.length == 9) args(8).toInt else -1
    val conf = new SparkConf()
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator",  classOf[ALSRegistrator].getName)
      .set("spark.kryo.referenceTracking", "false")
      .set("spark.kryoserializer.buffer.mb", "8")
      .set("spark.locality.wait", "10000")
    val sc = new SparkContext(master, "ALS", conf)

    val ratings = sc.textFile(ratingsFile).map { line =>
      val fields = line.split(',')
      Rating(fields(0).toInt, fields(1).toInt, fields(2).toDouble)
    }
    val model = new ALS(rank = rank, iterations = iters, lambda = lambda,
      numBlocks = blocks, implicitPrefs = implicitPrefs, alpha = alpha).run(ratings)

    model.userFeatures.map{ case (id, vec) => id + "," + vec.mkString(" ") }
                      .saveAsTextFile(outputDir + "/userFeatures")
    model.productFeatures.map{ case (id, vec) => id + "," + vec.mkString(" ") }
                         .saveAsTextFile(outputDir + "/productFeatures")
    println("Final user/product features written to " + outputDir)
    sc.stop()
  }
}
