package org.apache.spark.mllib.regression

import org.json4s.DefaultFormats
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

import scala.util.Random

import org.apache.spark.{SparkContext, Logging}
import org.apache.spark.mllib.linalg._
import org.apache.spark.mllib.optimization.{GradientDescent, Updater, Gradient}
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.mllib.util.Loader._
import org.apache.spark.mllib.util.{Loader, Saveable}
import org.apache.spark.sql.{DataFrame, SQLContext}

/**
 * Created by zrf on 4/13/15.
 */

object FMWithSGD {

  def train(input: RDD[LabeledPoint],
            numIterations: Int = 100,
            stepSize: Double = 0.01,
            miniBatchFraction: Double = 0.1,
            dim: (Boolean, Boolean, Int) = (true, true, 8),
            regParam: (Double, Double, Double) = (1, 1, 1),
            initStd: Double = 0.01): FMModel = {

    new FMWithSGD(stepSize, numIterations, dim, regParam, miniBatchFraction)
      .setInitStd(initStd)
      .run(input)
  }
}


class FMWithSGD(private var stepSize: Double,
                private var numIterations: Int,
                private var dim: (Boolean, Boolean, Int),
                private var regParam: (Double, Double, Double),
                private var miniBatchFraction: Double)
  extends Serializable with Logging {

  private var k0: Boolean = dim._1
  private var k1: Boolean = dim._2
  private var k2: Int = dim._3

  private var r0: Double = regParam._1
  private var r1: Double = regParam._2
  private var r2: Double = regParam._3

  private var initMean: Double = 0
  private var initStd: Double = 0.01

  private var numFeatures: Int = -1
  private var minLabel: Double = Double.MaxValue
  private var maxLabel: Double = Double.MinValue

  /**
   *
   */
  def setDim(dim: (Boolean, Boolean, Int)): this.type = {
    require(dim._3 > 0)
    this.k0 = dim._1
    this.k1 = dim._2
    this.k2 = dim._3
    this
  }

  /**
   *
   * @param addIntercept determines if the global bias term w0 should be used
   * @param add1Way determines if one-way interactions (bias terms for each variable)
   * @param numFactors the number of factors that are used for pairwise interactions
   * @return
   */
  def setDim(addIntercept: Boolean = true, add1Way: Boolean = true, numFactors: Int = 8): this.type = {
    setDim((addIntercept, add1Way, numFactors))
  }

  def setRegParam(reg: (Double, Double, Double)): this.type = {
    require(reg._1 >= 0 && reg._2 >= 0 && reg._3 >= 0)
    this.r0 = reg._1
    this.r1 = reg._2
    this.r2 = reg._3
    this
  }

  /**
   *
   * @param regIntercept intercept regularization
   * @param reg1Way one-way regularization
   * @param reg2Way two-way regularization
   * @return
   */
  def setRegParam(regIntercept: Double = 0, reg1Way: Double = 0, reg2Way: Double = 0): this.type = {
    setRegParam((regIntercept, reg1Way, reg2Way))
  }


  def setInitStd(initStd: Double): this.type = {
    require(initStd > 0)
    this.initStd = initStd
    this
  }

  /**
   * Set fraction of data to be used for each SGD iteration. Default 0.1.
   */
  def setMiniBatchFraction(miniBatchFraction: Double): this.type = {
    require(miniBatchFraction > 0 && miniBatchFraction <= 1)
    this.miniBatchFraction = miniBatchFraction
    this
  }

  /**
   * Set the number of iterations for SGD. Default 100.
   */
  def setNumIterations(numIterations: Int): this.type = {
    require(numIterations > 0)
    this.numIterations = numIterations
    this
  }

  /**
   * Set the initial step size of SGD for the first step. Default 0.01.
   * In subsequent steps, the step size will decrease with stepSize/sqrt(t)
   */
  def setStepSize(stepSize: Double): this.type = {
    require(stepSize >= 0)
    this.stepSize = stepSize
    this
  }


  /**
   * v : numFeatures * numFactors + w : numFeatures + w0 : 1
   * @return
   */
  private def genInitWeights(): Vector = {
    (k0, k1) match {
      case (true, true) =>
        Vectors.dense(Array.fill(numFeatures * k2)(Random.nextGaussian() * initStd + initMean) ++
          Array.fill(numFeatures + 1)(0.0))

      case (true, false) =>
        Vectors.dense(Array.fill(numFeatures * k2)(Random.nextGaussian() * initStd + initMean) ++
          Array(0.0))

      case (false, true) =>
        Vectors.dense(Array.fill(numFeatures * k2)(Random.nextGaussian() * initStd + initMean) ++
          Array.fill(numFeatures)(0.0))

      case (false, false) =>
        Vectors.dense(Array.fill(numFeatures * k2)(Random.nextGaussian() * initStd + initMean))
    }
  }

  private def createModel(weights: Vector): FMModel = {

    val values = weights.toArray

    val v = new DenseMatrix(k2, numFeatures, values.slice(0, numFeatures * k2))

    val w = if (k1)
      Some(Vectors.dense(values.slice(numFeatures * k2, numFeatures * k2 + numFeatures)))
    else None

    val w0 = if (k0) values.last else 0.0

    new FMModel(v, w, w0)
  }

  def run(input: RDD[LabeledPoint]): FMModel = {

    if (input.getStorageLevel == StorageLevel.NONE) {
      logWarning("The input data is not directly cached, which may hurt performance if its"
        + " parent RDDs are also uncached.")
    }

    this.numFeatures = input.first().features.size
    require(numFeatures > 0)

    val (minT, maxT) = input.map(_.label).aggregate[(Double, Double)]((Double.MaxValue, Double.MinValue))({
      case ((min, max), v) =>
        (Math.min(min, v), Math.max(max, v))
    }, {
      case ((min1, max1), (min2, max2)) =>
        (Math.min(min1, min2), Math.max(max1, max2))
    })

    this.minLabel = minT
    this.maxLabel = maxT

    val gradient = new FMSGDGradient(k0, k1, k2, numFeatures, minLabel, maxLabel)

    val updater = new FMSGDUpdater(k0, k1, k2, r0, r1, r2, numFeatures)

    val optimizer = new GradientDescent(gradient, updater)
      .setStepSize(stepSize)
      .setNumIterations(numIterations)
      .setMiniBatchFraction(miniBatchFraction)

    val data = input.map(l => (l.label, l.features)).cache()

    val initWeights = genInitWeights()

    val weights = optimizer.optimize(data, initWeights)

    createModel(weights)
  }
}


class FMSGDGradient(val k0: Boolean, val k1: Boolean, val k2: Int,
                    val numFeatures: Int, val min: Double, val max: Double) extends Gradient {

  private def predict(data: Vector, weights: Vector): (Double, Array[Double]) = {
    var pred = if (k0)
      weights(weights.size - 1)
    else 0.0

    if (k1) {
      val pos = numFeatures * k2
      data.foreachActive {
        case (i, v) =>
          pred += weights(pos + i) * v
      }
    }

    val sum = Array.fill(k2)(0.0)
    for (f <- 0 until k2) {
      var sumSqr = 0.0
      data.foreachActive {
        case (i, v) =>
          val d = weights(i * k2 + f) * v
          sum(f) += d
          sumSqr += d * d
      }
      pred += (sum(f) * sum(f) - sumSqr) / 2
    }

    pred = Math.max(pred, min)
    pred = Math.min(pred, max)

    (pred, sum)
  }


  private def cumulateGradient(data: Vector, weights: Vector,
                               diff: Double, sum: Array[Double], cumGrad: Vector): Unit = {
    cumGrad match {
      case vec: DenseVector =>
        val cumValues = vec.values

        if (k0)
          cumValues(cumValues.length - 1) += diff

        if (k1) {
          val pos = numFeatures * k2
          data.foreachActive {
            case (i, v) =>
              cumValues(pos + i) += v * diff
          }
        }

        data.foreachActive {
          case (i, v) =>
            val pos = i * k2
            for (f <- 0 until k2) {
              cumValues(pos + f) += (sum(f) * v - weights(pos + f) * v * v) * diff
            }
        }

      case _ =>
        throw new IllegalArgumentException(
          s"cumulateGradient only supports adding to a dense vector but got type ${cumGrad.getClass}.")
    }
  }


  override def compute(data: Vector, label: Double, weights: Vector): (Vector, Double) = {
    val cumGradient = Vectors.dense(Array.fill(weights.size)(0.0))
    val loss = compute(data, label, weights, cumGradient)
    (cumGradient, loss)
  }

  override def compute(data: Vector, label: Double, weights: Vector, cumGradient: Vector): Double = {
    require(data.size == numFeatures)
    val (pred, sum) = predict(data, weights)
    val diff = pred - label
    cumulateGradient(data, weights, diff, sum, cumGradient)
    diff * diff / 2
  }
}


class FMSGDUpdater(val k0: Boolean, val k1: Boolean, val k2: Int,
                   val r0: Double, val r1: Double, val r2: Double,
                   val numFeatures: Int) extends Updater {

  override def compute(weightsOld: Vector, gradient: Vector,
                       stepSize: Double, iter: Int, regParam: Double): (Vector, Double) = {
    val thisIterStepSize = stepSize / math.sqrt(iter)
    //    val thisIterStepSize = stepSize
    val len = weightsOld.size

    val weightsNew = Array.fill(len)(0.0)
    var regVal = 0.0

    if (k0) {
      weightsNew(len - 1) = weightsOld(len - 1) - thisIterStepSize * (gradient(len - 1) + r0 * weightsOld(len - 1))
      regVal += r0 * weightsNew(len - 1) * weightsNew(len - 1)
    }

    if (k1) {
      for (i <- numFeatures * k2 until numFeatures * k2 + numFeatures) {
        weightsNew(i) = weightsOld(i) - thisIterStepSize * (gradient(i) + r1 * weightsOld(i))
        regVal += r1 * weightsNew(i) * weightsNew(i)
      }
    }

    for (i <- 0 until numFeatures * k2) {
      weightsNew(i) = weightsOld(i) - thisIterStepSize * (gradient(i) + r2 * weightsOld(i))
      regVal += r2 * weightsNew(i) * weightsNew(i)
    }

    (Vectors.dense(weightsNew), regVal / 2)
  }
}


class FMModel(val factorMatrix: Matrix,
              val weightVector: Option[Vector],
              val intercept: Double) extends Serializable with Saveable {

  val numFeatures = factorMatrix.numCols
  val numFactors = factorMatrix.numRows

  def predict(testData: Vector): Double = {
    require(testData.size == numFeatures)

    var pred = intercept
    if (weightVector.isDefined) {
      testData.foreachActive {
        case (i, v) =>
          pred += weightVector.get(i) * v
      }
    }

    for (f <- 0 until numFactors) {
      var sum = 0.0
      var sumSqr = 0.0
      testData.foreachActive {
        case (i, v) =>
          val d = factorMatrix(f, i) * v
          sum += d
          sumSqr += d * d
      }
      pred += (sum * sum - sumSqr) / 2
    }

    pred
  }

  def predict(testData: RDD[Vector]): RDD[Double] = {
    testData.mapPartitions {
      _.map {
        vec =>
          predict(vec)
      }
    }
  }

  override protected def formatVersion: String = "1.0"

  override def save(sc: SparkContext, path: String): Unit = {
    val data = FMModel.SaveLoadV1_0.Data(factorMatrix, weightVector, intercept)
    FMModel.SaveLoadV1_0.save(sc, path, data)
  }
}

object FMModel extends Loader[FMModel] {

  private object SaveLoadV1_0 {

    def thisFormatVersion = "1.0"

    def thisClassName = this.getClass.getName

    /** Model data for model import/export */
    case class Data(factorMatrix: Matrix, weightVector: Option[Vector], intercept: Double)

    def save(sc: SparkContext, path: String, data: Data): Unit = {
      val sqlContext = new SQLContext(sc)
      import sqlContext.implicits._
      // Create JSON metadata.
      val metadata = compact(render(
        ("class" -> this.getClass.getName) ~ ("version" -> thisFormatVersion) ~
          ("numFeatures" -> data.factorMatrix.numCols) ~ ("numFactors" -> data.factorMatrix.numRows)))
      sc.parallelize(Seq(metadata), 1).saveAsTextFile(metadataPath(path))

      // Create Parquet data.
      val dataRDD: DataFrame = sc.parallelize(Seq(data), 1).toDF()
      dataRDD.saveAsParquetFile(dataPath(path))
    }

    def load(sc: SparkContext, path: String): FMModel = {
      val sqlContext = new SQLContext(sc)
      // Load Parquet data.
      val dataRDD = sqlContext.parquetFile(dataPath(path))
      // Check schema explicitly since erasure makes it hard to use match-case for checking.
      checkSchema[Data](dataRDD.schema)
      val dataArray = dataRDD.select("factorMatrix", "weightVector", "intercept").take(1)
      assert(dataArray.length == 1, s"Unable to load FMModel data from: ${dataPath(path)}")
      val data = dataArray(0)
      val factorMatrix = data.getAs[Matrix](0)
      val weightVector = data.getAs[Option[Vector]](1)
      val intercept = data.getDouble(2)
      new FMModel(factorMatrix, weightVector, intercept)
    }
  }

  override def load(sc: SparkContext, path: String): FMModel = {
    implicit val formats = DefaultFormats

    val (loadedClassName, version, metadata) = loadMetadata(sc, path)
    val classNameV1_0 = SaveLoadV1_0.thisClassName

    (loadedClassName, version) match {
      case (className, "1.0") if className == classNameV1_0 =>
        val numFeatures = (metadata \ "numFeatures").extract[Int]
        val numFactors = (metadata \ "numFactors").extract[Int]
        val model = SaveLoadV1_0.load(sc, path)
        assert(model.factorMatrix.numCols == numFeatures,
          s"FMModel.load expected $numFeatures features," +
            s" but factorMatrix had columns of size:" +
            s" ${model.factorMatrix.numCols}")
        assert(model.factorMatrix.numRows == numFactors,
          s"FMModel.load expected $numFactors factors," +
            s" but factorMatrix had rows of size:" +
            s" ${model.factorMatrix.numRows}")
        model

      case _ => throw new Exception(
        s"FMModel.load did not recognize model with (className, format version):" +
          s"($loadedClassName, $version).  Supported:\n" +
          s"  ($classNameV1_0, 1.0)")
    }
  }
}

