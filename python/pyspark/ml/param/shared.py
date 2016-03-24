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

    def setMaxIter(self, value):
        """
        Sets the value of :py:attr:`maxIter`.
        """
        self._set(maxIter=value)
        return self

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

    def setRegParam(self, value):
        """
        Sets the value of :py:attr:`regParam`.
        """
        self._set(regParam=value)
        return self

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

    def setFeaturesCol(self, value):
        """
        Sets the value of :py:attr:`featuresCol`.
        """
        self._set(featuresCol=value)
        return self

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

    def setLabelCol(self, value):
        """
        Sets the value of :py:attr:`labelCol`.
        """
        self._set(labelCol=value)
        return self

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

    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        self._set(predictionCol=value)
        return self

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

    def setProbabilityCol(self, value):
        """
        Sets the value of :py:attr:`probabilityCol`.
        """
        self._set(probabilityCol=value)
        return self

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

    def setRawPredictionCol(self, value):
        """
        Sets the value of :py:attr:`rawPredictionCol`.
        """
        self._set(rawPredictionCol=value)
        return self

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

    def setInputCol(self, value):
        """
        Sets the value of :py:attr:`inputCol`.
        """
        self._set(inputCol=value)
        return self

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

    def setInputCols(self, value):
        """
        Sets the value of :py:attr:`inputCols`.
        """
        self._set(inputCols=value)
        return self

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

    def setOutputCol(self, value):
        """
        Sets the value of :py:attr:`outputCol`.
        """
        self._set(outputCol=value)
        return self

    def getOutputCol(self):
        """
        Gets the value of outputCol or its default value.
        """
        return self.getOrDefault(self.outputCol)


class HasNumFeatures(Params):
    """
    Mixin for param numFeatures: number of features.
    """

    numFeatures = Param(Params._dummy(), "numFeatures", "number of features.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasNumFeatures, self).__init__()

    def setNumFeatures(self, value):
        """
        Sets the value of :py:attr:`numFeatures`.
        """
        self._set(numFeatures=value)
        return self

    def getNumFeatures(self):
        """
        Gets the value of numFeatures or its default value.
        """
        return self.getOrDefault(self.numFeatures)


class HasCheckpointInterval(Params):
    """
    Mixin for param checkpointInterval: set checkpoint interval (>= 1) or disable checkpoint (-1). E.g. 10 means that the cache will get checkpointed every 10 iterations.
    """

    checkpointInterval = Param(Params._dummy(), "checkpointInterval", "set checkpoint interval (>= 1) or disable checkpoint (-1). E.g. 10 means that the cache will get checkpointed every 10 iterations.", typeConverter=TypeConverters.toInt)

    def __init__(self):
        super(HasCheckpointInterval, self).__init__()

    def setCheckpointInterval(self, value):
        """
        Sets the value of :py:attr:`checkpointInterval`.
        """
        self._set(checkpointInterval=value)
        return self

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

    def setSeed(self, value):
        """
        Sets the value of :py:attr:`seed`.
        """
        self._set(seed=value)
        return self

    def getSeed(self):
        """
        Gets the value of seed or its default value.
        """
        return self.getOrDefault(self.seed)


class HasTol(Params):
    """
    Mixin for param tol: the convergence tolerance for iterative algorithms.
    """

    tol = Param(Params._dummy(), "tol", "the convergence tolerance for iterative algorithms.", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasTol, self).__init__()

    def setTol(self, value):
        """
        Sets the value of :py:attr:`tol`.
        """
        self._set(tol=value)
        return self

    def getTol(self):
        """
        Gets the value of tol or its default value.
        """
        return self.getOrDefault(self.tol)


class HasStepSize(Params):
    """
    Mixin for param stepSize: Step size to be used for each iteration of optimization.
    """

    stepSize = Param(Params._dummy(), "stepSize", "Step size to be used for each iteration of optimization.", typeConverter=TypeConverters.toFloat)

    def __init__(self):
        super(HasStepSize, self).__init__()

    def setStepSize(self, value):
        """
        Sets the value of :py:attr:`stepSize`.
        """
        self._set(stepSize=value)
        return self

    def getStepSize(self):
        """
        Gets the value of stepSize or its default value.
        """
        return self.getOrDefault(self.stepSize)


class HasHandleInvalid(Params):
    """
    Mixin for param handleInvalid: how to handle invalid entries. Options are skip (which will filter out rows with bad values), or error (which will throw an errror). More options may be added later.
    """

    handleInvalid = Param(Params._dummy(), "handleInvalid", "how to handle invalid entries. Options are skip (which will filter out rows with bad values), or error (which will throw an errror). More options may be added later.", typeConverter=TypeConverters.toBoolean)

    def __init__(self):
        super(HasHandleInvalid, self).__init__()

    def setHandleInvalid(self, value):
        """
        Sets the value of :py:attr:`handleInvalid`.
        """
        self._set(handleInvalid=value)
        return self

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

    def setElasticNetParam(self, value):
        """
        Sets the value of :py:attr:`elasticNetParam`.
        """
        self._set(elasticNetParam=value)
        return self

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

    def setFitIntercept(self, value):
        """
        Sets the value of :py:attr:`fitIntercept`.
        """
        self._set(fitIntercept=value)
        return self

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

    def setStandardization(self, value):
        """
        Sets the value of :py:attr:`standardization`.
        """
        self._set(standardization=value)
        return self

    def getStandardization(self):
        """
        Gets the value of standardization or its default value.
        """
        return self.getOrDefault(self.standardization)


class HasThresholds(Params):
    """
    Mixin for param thresholds: Thresholds in multi-class classification to adjust the probability of predicting each class. Array must have length equal to the number of classes, with values >= 0. The class with largest value p/t is predicted, where p is the original probability of that class and t is the class' threshold.
    """

    thresholds = Param(Params._dummy(), "thresholds", "Thresholds in multi-class classification to adjust the probability of predicting each class. Array must have length equal to the number of classes, with values >= 0. The class with largest value p/t is predicted, where p is the original probability of that class and t is the class' threshold.", typeConverter=TypeConverters.toListFloat)

    def __init__(self):
        super(HasThresholds, self).__init__()

    def setThresholds(self, value):
        """
        Sets the value of :py:attr:`thresholds`.
        """
        self._set(thresholds=value)
        return self

    def getThresholds(self):
        """
        Gets the value of thresholds or its default value.
        """
        return self.getOrDefault(self.thresholds)


class HasWeightCol(Params):
    """
    Mixin for param weightCol: weight column name. If this is not set or empty, we treat all instance weights as 1.0.
    """

    weightCol = Param(Params._dummy(), "weightCol", "weight column name. If this is not set or empty, we treat all instance weights as 1.0.", typeConverter=TypeConverters.toString)

    def __init__(self):
        super(HasWeightCol, self).__init__()

    def setWeightCol(self, value):
        """
        Sets the value of :py:attr:`weightCol`.
        """
        self._set(weightCol=value)
        return self

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

    def setSolver(self, value):
        """
        Sets the value of :py:attr:`solver`.
        """
        self._set(solver=value)
        return self

    def getSolver(self):
        """
        Gets the value of solver or its default value.
        """
        return self.getOrDefault(self.solver)


class DecisionTreeParams(Params):
    """
    Mixin for Decision Tree parameters.
    """

    maxDepth = Param(Params._dummy(), "maxDepth", "Maximum depth of the tree. (>= 0) E.g., depth 0 means 1 leaf node; depth 1 means 1 internal node + 2 leaf nodes.", typeConverter=TypeConverters.toInt)
    maxBins = Param(Params._dummy(), "maxBins", "Max number of bins for discretizing continuous features.  Must be >=2 and >= number of categories for any categorical feature.", typeConverter=TypeConverters.toInt)
    minInstancesPerNode = Param(Params._dummy(), "minInstancesPerNode", "Minimum number of instances each child must have after split. If a split causes the left or right child to have fewer than minInstancesPerNode, the split will be discarded as invalid. Should be >= 1.", typeConverter=TypeConverters.toInt)
    minInfoGain = Param(Params._dummy(), "minInfoGain", "Minimum information gain for a split to be considered at a tree node.", typeConverter=TypeConverters.toFloat)
    maxMemoryInMB = Param(Params._dummy(), "maxMemoryInMB", "Maximum memory in MB allocated to histogram aggregation.", typeConverter=TypeConverters.toInt)
    cacheNodeIds = Param(Params._dummy(), "cacheNodeIds", "If false, the algorithm will pass trees to executors to match instances with nodes. If true, the algorithm will cache node IDs for each instance. Caching can speed up training of deeper trees. Users can set how often should the cache be checkpointed or disable it by setting checkpointInterval.", typeConverter=TypeConverters.toBoolean)
    

    def __init__(self):
        super(DecisionTreeParams, self).__init__()

    def setMaxDepth(self, value):
        """
        Sets the value of :py:attr:`maxDepth`.
        """
        self._set(maxDepth=value)
        return self

    def getMaxDepth(self):
        """
        Gets the value of maxDepth or its default value.
        """
        return self.getOrDefault(self.maxDepth)

    def setMaxBins(self, value):
        """
        Sets the value of :py:attr:`maxBins`.
        """
        self._set(maxBins=value)
        return self

    def getMaxBins(self):
        """
        Gets the value of maxBins or its default value.
        """
        return self.getOrDefault(self.maxBins)

    def setMinInstancesPerNode(self, value):
        """
        Sets the value of :py:attr:`minInstancesPerNode`.
        """
        self._set(minInstancesPerNode=value)
        return self

    def getMinInstancesPerNode(self):
        """
        Gets the value of minInstancesPerNode or its default value.
        """
        return self.getOrDefault(self.minInstancesPerNode)

    def setMinInfoGain(self, value):
        """
        Sets the value of :py:attr:`minInfoGain`.
        """
        self._set(minInfoGain=value)
        return self

    def getMinInfoGain(self):
        """
        Gets the value of minInfoGain or its default value.
        """
        return self.getOrDefault(self.minInfoGain)

    def setMaxMemoryInMB(self, value):
        """
        Sets the value of :py:attr:`maxMemoryInMB`.
        """
        self._set(maxMemoryInMB=value)
        return self

    def getMaxMemoryInMB(self):
        """
        Gets the value of maxMemoryInMB or its default value.
        """
        return self.getOrDefault(self.maxMemoryInMB)

    def setCacheNodeIds(self, value):
        """
        Sets the value of :py:attr:`cacheNodeIds`.
        """
        self._set(cacheNodeIds=value)
        return self

    def getCacheNodeIds(self):
        """
        Gets the value of cacheNodeIds or its default value.
        """
        return self.getOrDefault(self.cacheNodeIds)

