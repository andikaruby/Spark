#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# DO NOT MODIFY THIS FILE! It was generated by _shared_params_code_gen.py.

from pyspark.ml.param import *


class HasMaxIter(Params):
    """
    Mixin for param maxIter: max number of iterations (>= 0).
    """

    maxIter = Param(Params._dummy(), "maxIter", "max number of iterations (>= 0).", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasMaxIter, self).__init__()

    def getMaxIter(self):
        """
        Gets the value of maxIter or its default value.
        """
        return self.getOrDefault(self.maxIter)


class HasRegParam(Params):
    """
    Mixin for param regParam: regularization parameter (>= 0).
    """

    regParam = Param(Params._dummy(), "regParam", "regularization parameter (>= 0).", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasRegParam, self).__init__()

    def getRegParam(self):
        """
        Gets the value of regParam or its default value.
        """
        return self.getOrDefault(self.regParam)


class HasFeaturesCol(Params):
    """
    Mixin for param featuresCol: features column name.
    """

    featuresCol = Param(Params._dummy(), "featuresCol", "features column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasFeaturesCol, self).__init__()
        self._setDefault(featuresCol='features')

    def getFeaturesCol(self):
        """
        Gets the value of featuresCol or its default value.
        """
        return self.getOrDefault(self.featuresCol)


class HasLabelCol(Params):
    """
    Mixin for param labelCol: label column name.
    """

    labelCol = Param(Params._dummy(), "labelCol", "label column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasLabelCol, self).__init__()
        self._setDefault(labelCol='label')

    def getLabelCol(self):
        """
        Gets the value of labelCol or its default value.
        """
        return self.getOrDefault(self.labelCol)


class HasPredictionCol(Params):
    """
    Mixin for param predictionCol: prediction column name.
    """

    predictionCol = Param(Params._dummy(), "predictionCol", "prediction column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasPredictionCol, self).__init__()
        self._setDefault(predictionCol='prediction')

    def getPredictionCol(self):
        """
        Gets the value of predictionCol or its default value.
        """
        return self.getOrDefault(self.predictionCol)


class HasProbabilityCol(Params):
    """
    Mixin for param probabilityCol: Column name for predicted class conditional probabilities. Note: Not all models output well-calibrated probability estimates! These probabilities should be treated as confidences, not precise probabilities.
    """

    probabilityCol = Param(Params._dummy(), "probabilityCol", "Column name for predicted class conditional probabilities. Note: Not all models output well-calibrated probability estimates! These probabilities should be treated as confidences, not precise probabilities.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasProbabilityCol, self).__init__()
        self._setDefault(probabilityCol='probability')

    def getProbabilityCol(self):
        """
        Gets the value of probabilityCol or its default value.
        """
        return self.getOrDefault(self.probabilityCol)


class HasRawPredictionCol(Params):
    """
    Mixin for param rawPredictionCol: raw prediction (a.k.a. confidence) column name.
    """

    rawPredictionCol = Param(Params._dummy(), "rawPredictionCol", "raw prediction (a.k.a. confidence) column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasRawPredictionCol, self).__init__()
        self._setDefault(rawPredictionCol='rawPrediction')

    def getRawPredictionCol(self):
        """
        Gets the value of rawPredictionCol or its default value.
        """
        return self.getOrDefault(self.rawPredictionCol)


class HasInputCol(Params):
    """
    Mixin for param inputCol: input column name.
    """

    inputCol = Param(Params._dummy(), "inputCol", "input column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasInputCol, self).__init__()

    def getInputCol(self):
        """
        Gets the value of inputCol or its default value.
        """
        return self.getOrDefault(self.inputCol)


class HasInputCols(Params):
    """
    Mixin for param inputCols: input column names.
    """

    inputCols = Param(Params._dummy(), "inputCols", "input column names.", typeConverter=TypeConverters.toListString)

    def __init__(self):
        super(HasInputCols, self).__init__()

    def getInputCols(self):
        """
        Gets the value of inputCols or its default value.
        """
        return self.getOrDefault(self.inputCols)


class HasOutputCol(Params):
    """
    Mixin for param outputCol: output column name.
    """

    outputCol = Param(Params._dummy(), "outputCol", "output column name.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasOutputCol, self).__init__()
        self._setDefault(outputCol=self.uid + '__output')

    def getOutputCol(self):
        """
        Gets the value of outputCol or its default value.
        """
        return self.getOrDefault(self.outputCol)


class HasOutputCols(Params):
    """
    Mixin for param outputCols: output column names.
    """

    outputCols = Param(Params._dummy(), "outputCols", "output column names.", typeConverter=TypeConverters.toListString)

    def __init__(self):
        super(HasOutputCols, self).__init__()

    def getOutputCols(self):
        """
        Gets the value of outputCols or its default value.
        """
        return self.getOrDefault(self.outputCols)


class HasNumFeatures(Params):
    """
    Mixin for param numFeatures: Number of features. Should be greater than 0.
    """

    numFeatures = Param(Params._dummy(), "numFeatures", "Number of features. Should be greater than 0.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasNumFeatures, self).__init__()
        self._setDefault(numFeatures=262144)

    def getNumFeatures(self):
        """
        Gets the value of numFeatures or its default value.
        """
        return self.getOrDefault(self.numFeatures)


class HasCheckpointInterval(Params):
    """
    Mixin for param checkpointInterval: set checkpoint interval (>= 1) or disable checkpoint (-1). E.g. 10 means that the cache will get checkpointed every 10 iterations. Note: this setting will be ignored if the checkpoint directory is not set in the SparkContext.
    """

    checkpointInterval = Param(Params._dummy(), "checkpointInterval", "set checkpoint interval (>= 1) or disable checkpoint (-1). E.g. 10 means that the cache will get checkpointed every 10 iterations. Note: this setting will be ignored if the checkpoint directory is not set in the SparkContext.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasCheckpointInterval, self).__init__()

    def getCheckpointInterval(self):
        """
        Gets the value of checkpointInterval or its default value.
        """
        return self.getOrDefault(self.checkpointInterval)


class HasSeed(Params):
    """
    Mixin for param seed: random seed.
    """

    seed = Param(Params._dummy(), "seed", "random seed.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasSeed, self).__init__()
        self._setDefault(seed=hash(type(self).__name__))

    def getSeed(self):
        """
        Gets the value of seed or its default value.
        """
        return self.getOrDefault(self.seed)


class HasTol(Params):
    """
    Mixin for param tol: the convergence tolerance for iterative algorithms (>= 0).
    """

    tol = Param(Params._dummy(), "tol", "the convergence tolerance for iterative algorithms (>= 0).", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasTol, self).__init__()

    def getTol(self):
        """
        Gets the value of tol or its default value.
        """
        return self.getOrDefault(self.tol)


class HasRelativeError(Params):
    """
    Mixin for param relativeError: the relative target precision for the approximate quantile algorithm. Must be in the range [0, 1]
    """

    relativeError = Param(Params._dummy(), "relativeError", "the relative target precision for the approximate quantile algorithm. Must be in the range [0, 1]", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasRelativeError, self).__init__()
        self._setDefault(relativeError=0.001)

    def getRelativeError(self):
        """
        Gets the value of relativeError or its default value.
        """
        return self.getOrDefault(self.relativeError)


class HasStepSize(Params):
    """
    Mixin for param stepSize: Step size to be used for each iteration of optimization (>= 0).
    """

    stepSize = Param(Params._dummy(), "stepSize", "Step size to be used for each iteration of optimization (>= 0).", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasStepSize, self).__init__()

    def getStepSize(self):
        """
        Gets the value of stepSize or its default value.
        """
        return self.getOrDefault(self.stepSize)


class HasHandleInvalid(Params):
    """
    Mixin for param handleInvalid: how to handle invalid entries. Options are skip (which will filter out rows with bad values), or error (which will throw an error). More options may be added later.
    """

    handleInvalid = Param(Params._dummy(), "handleInvalid", "how to handle invalid entries. Options are skip (which will filter out rows with bad values), or error (which will throw an error). More options may be added later.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasHandleInvalid, self).__init__()

    def getHandleInvalid(self):
        """
        Gets the value of handleInvalid or its default value.
        """
        return self.getOrDefault(self.handleInvalid)


class HasElasticNetParam(Params):
    """
    Mixin for param elasticNetParam: the ElasticNet mixing parameter, in range [0, 1]. For alpha = 0, the penalty is an L2 penalty. For alpha = 1, it is an L1 penalty.
    """

    elasticNetParam = Param(Params._dummy(), "elasticNetParam", "the ElasticNet mixing parameter, in range [0, 1]. For alpha = 0, the penalty is an L2 penalty. For alpha = 1, it is an L1 penalty.", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasElasticNetParam, self).__init__()
        self._setDefault(elasticNetParam=0.0)

    def getElasticNetParam(self):
        """
        Gets the value of elasticNetParam or its default value.
        """
        return self.getOrDefault(self.elasticNetParam)


class HasFitIntercept(Params):
    """
    Mixin for param fitIntercept: whether to fit an intercept term.
    """

    fitIntercept = Param(Params._dummy(), "fitIntercept", "whether to fit an intercept term.", typeConverter=TypeConverters.toBoolean)

    def __init__(self):
        super(HasFitIntercept, self).__init__()
        self._setDefault(fitIntercept=True)

    def getFitIntercept(self):
        """
        Gets the value of fitIntercept or its default value.
        """
        return self.getOrDefault(self.fitIntercept)


class HasStandardization(Params):
    """
    Mixin for param standardization: whether to standardize the training features before fitting the model.
    """

    standardization = Param(Params._dummy(), "standardization", "whether to standardize the training features before fitting the model.", typeConverter=TypeConverters.toBoolean)

    def __init__(self):
        super(HasStandardization, self).__init__()
        self._setDefault(standardization=True)

    def getStandardization(self):
        """
        Gets the value of standardization or its default value.
        """
        return self.getOrDefault(self.standardization)


class HasThresholds(Params):
    """
    Mixin for param thresholds: Thresholds in multi-class classification to adjust the probability of predicting each class. Array must have length equal to the number of classes, with values > 0, excepting that at most one value may be 0. The class with largest value p/t is predicted, where p is the original probability of that class and t is the class's threshold.
    """

    thresholds = Param(Params._dummy(), "thresholds", "Thresholds in multi-class classification to adjust the probability of predicting each class. Array must have length equal to the number of classes, with values > 0, excepting that at most one value may be 0. The class with largest value p/t is predicted, where p is the original probability of that class and t is the class's threshold.", typeConverter=TypeConverters.toListFloat)

    def __init__(self):
        super(HasThresholds, self).__init__()

    def getThresholds(self):
        """
        Gets the value of thresholds or its default value.
        """
        return self.getOrDefault(self.thresholds)


class HasThreshold(Params):
    """
    Mixin for param threshold: threshold in binary classification prediction, in range [0, 1]
    """

    threshold = Param(Params._dummy(), "threshold", "threshold in binary classification prediction, in range [0, 1]", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasThreshold, self).__init__()
        self._setDefault(threshold=0.5)

    def getThreshold(self):
        """
        Gets the value of threshold or its default value.
        """
        return self.getOrDefault(self.threshold)


class HasWeightCol(Params):
    """
    Mixin for param weightCol: weight column name. If this is not set or empty, we treat all instance weights as 1.0.
    """

    weightCol = Param(Params._dummy(), "weightCol", "weight column name. If this is not set or empty, we treat all instance weights as 1.0.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasWeightCol, self).__init__()

    def getWeightCol(self):
        """
        Gets the value of weightCol or its default value.
        """
        return self.getOrDefault(self.weightCol)


class HasSolver(Params):
    """
    Mixin for param solver: the solver algorithm for optimization. If this is not set or empty, default value is 'auto'.
    """

    solver = Param(Params._dummy(), "solver", "the solver algorithm for optimization. If this is not set or empty, default value is 'auto'.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasSolver, self).__init__()
        self._setDefault(solver='auto')

    def getSolver(self):
        """
        Gets the value of solver or its default value.
        """
        return self.getOrDefault(self.solver)


class HasVarianceCol(Params):
    """
    Mixin for param varianceCol: column name for the biased sample variance of prediction.
    """

    varianceCol = Param(Params._dummy(), "varianceCol", "column name for the biased sample variance of prediction.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasVarianceCol, self).__init__()

    def getVarianceCol(self):
        """
        Gets the value of varianceCol or its default value.
        """
        return self.getOrDefault(self.varianceCol)


class HasAggregationDepth(Params):
    """
    Mixin for param aggregationDepth: suggested depth for treeAggregate (>= 2).
    """

    aggregationDepth = Param(Params._dummy(), "aggregationDepth", "suggested depth for treeAggregate (>= 2).", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasAggregationDepth, self).__init__()
        self._setDefault(aggregationDepth=2)

    def getAggregationDepth(self):
        """
        Gets the value of aggregationDepth or its default value.
        """
        return self.getOrDefault(self.aggregationDepth)


class HasParallelism(Params):
    """
    Mixin for param parallelism: the number of threads to use when running parallel algorithms (>= 1).
    """

    parallelism = Param(Params._dummy(), "parallelism", "the number of threads to use when running parallel algorithms (>= 1).", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasParallelism, self).__init__()
        self._setDefault(parallelism=1)

    def getParallelism(self):
        """
        Gets the value of parallelism or its default value.
        """
        return self.getOrDefault(self.parallelism)


class HasCollectSubModels(Params):
    """
    Mixin for param collectSubModels: Param for whether to collect a list of sub-models trained during tuning. If set to false, then only the single best sub-model will be available after fitting. If set to true, then all sub-models will be available. Warning: For large models, collecting all sub-models can cause OOMs on the Spark driver.
    """

    collectSubModels = Param(Params._dummy(), "collectSubModels", "Param for whether to collect a list of sub-models trained during tuning. If set to false, then only the single best sub-model will be available after fitting. If set to true, then all sub-models will be available. Warning: For large models, collecting all sub-models can cause OOMs on the Spark driver.", typeConverter=TypeConverters.toBoolean)

    def __init__(self):
        super(HasCollectSubModels, self).__init__()
        self._setDefault(collectSubModels=False)

    def getCollectSubModels(self):
        """
        Gets the value of collectSubModels or its default value.
        """
        return self.getOrDefault(self.collectSubModels)


class HasLoss(Params):
    """
    Mixin for param loss: the loss function to be optimized.
    """

    loss = Param(Params._dummy(), "loss", "the loss function to be optimized.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasLoss, self).__init__()

    def getLoss(self):
        """
        Gets the value of loss or its default value.
        """
        return self.getOrDefault(self.loss)


class HasDistanceMeasure(Params):
    """
    Mixin for param distanceMeasure: the distance measure. Supported options: 'euclidean' and 'cosine'.
    """

    distanceMeasure = Param(Params._dummy(), "distanceMeasure", "the distance measure. Supported options: 'euclidean' and 'cosine'.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasDistanceMeasure, self).__init__()
        self._setDefault(distanceMeasure='euclidean')

    def getDistanceMeasure(self):
        """
        Gets the value of distanceMeasure or its default value.
        """
        return self.getOrDefault(self.distanceMeasure)


class HasValidationIndicatorCol(Params):
    """
    Mixin for param validationIndicatorCol: name of the column that indicates whether each row is for training or for validation. False indicates training; true indicates validation.
    """

    validationIndicatorCol = Param(Params._dummy(), "validationIndicatorCol", "name of the column that indicates whether each row is for training or for validation. False indicates training; true indicates validation.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasValidationIndicatorCol, self).__init__()

    def getValidationIndicatorCol(self):
        """
        Gets the value of validationIndicatorCol or its default value.
        """
        return self.getOrDefault(self.validationIndicatorCol)


class HasBlockSize(Params):
    """
    Mixin for param blockSize: block size for stacking input data in matrices. Data is stacked within partitions. If block size is more than remaining data in a partition then it is adjusted to the size of this data.
    """

    blockSize = Param(Params._dummy(), "blockSize", "block size for stacking input data in matrices. Data is stacked within partitions. If block size is more than remaining data in a partition then it is adjusted to the size of this data.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasBlockSize, self).__init__()
        self._setDefault(blockSize=1024)

    def getBlockSize(self):
        """
        Gets the value of blockSize or its default value.
        """
        return self.getOrDefault(self.blockSize)
