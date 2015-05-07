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

from pyspark.ml.param import Param, Params


class HasMaxIter(Params):
    """
    Mixin for param maxIter: max number of iterations.
    """

    # a placeholder to make it appear in the generated doc
    maxIter = Param(Params._dummy(), "maxIter", "max number of iterations")

    def __init__(self):
        super(HasMaxIter, self).__init__()
        #: param for max number of iterations
        self.maxIter = Param(self, "maxIter", "max number of iterations")
        if None is not None:
            self._setDefault(maxIter=None)

    def setMaxIter(self, value):
        """
        Sets the value of :py:attr:`maxIter`.
        """
        self.paramMap[self.maxIter] = value
        return self

    def getMaxIter(self):
        """
        Gets the value of maxIter or its default value.
        """
        return self.getOrDefault(self.maxIter)


class HasRegParam(Params):
    """
    Mixin for param regParam: regularization constant.
    """

    # a placeholder to make it appear in the generated doc
    regParam = Param(Params._dummy(), "regParam", "regularization constant")

    def __init__(self):
        super(HasRegParam, self).__init__()
        #: param for regularization constant
        self.regParam = Param(self, "regParam", "regularization constant")
        if None is not None:
            self._setDefault(regParam=None)

    def setRegParam(self, value):
        """
        Sets the value of :py:attr:`regParam`.
        """
        self.paramMap[self.regParam] = value
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

    # a placeholder to make it appear in the generated doc
    featuresCol = Param(Params._dummy(), "featuresCol", "features column name")

    def __init__(self):
        super(HasFeaturesCol, self).__init__()
        #: param for features column name
        self.featuresCol = Param(self, "featuresCol", "features column name")
        if 'features' is not None:
            self._setDefault(featuresCol='features')

    def setFeaturesCol(self, value):
        """
        Sets the value of :py:attr:`featuresCol`.
        """
        self.paramMap[self.featuresCol] = value
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

    # a placeholder to make it appear in the generated doc
    labelCol = Param(Params._dummy(), "labelCol", "label column name")

    def __init__(self):
        super(HasLabelCol, self).__init__()
        #: param for label column name
        self.labelCol = Param(self, "labelCol", "label column name")
        if 'label' is not None:
            self._setDefault(labelCol='label')

    def setLabelCol(self, value):
        """
        Sets the value of :py:attr:`labelCol`.
        """
        self.paramMap[self.labelCol] = value
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

    # a placeholder to make it appear in the generated doc
    predictionCol = Param(Params._dummy(), "predictionCol", "prediction column name")

    def __init__(self):
        super(HasPredictionCol, self).__init__()
        #: param for prediction column name
        self.predictionCol = Param(self, "predictionCol", "prediction column name")
        if 'prediction' is not None:
            self._setDefault(predictionCol='prediction')

    def setPredictionCol(self, value):
        """
        Sets the value of :py:attr:`predictionCol`.
        """
        self.paramMap[self.predictionCol] = value
        return self

    def getPredictionCol(self):
        """
        Gets the value of predictionCol or its default value.
        """
        return self.getOrDefault(self.predictionCol)


class HasRawPredictionCol(Params):
    """
    Mixin for param rawPredictionCol: raw prediction column name.
    """

    # a placeholder to make it appear in the generated doc
    rawPredictionCol = Param(Params._dummy(), "rawPredictionCol", "raw prediction column name")

    def __init__(self):
        super(HasRawPredictionCol, self).__init__()
        #: param for raw prediction column name
        self.rawPredictionCol = Param(self, "rawPredictionCol", "raw prediction column name")
        if 'rawPrediction' is not None:
            self._setDefault(rawPredictionCol='rawPrediction')

    def setRawPredictionCol(self, value):
        """
        Sets the value of :py:attr:`rawPredictionCol`.
        """
        self.paramMap[self.rawPredictionCol] = value
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

    # a placeholder to make it appear in the generated doc
    inputCol = Param(Params._dummy(), "inputCol", "input column name")

    def __init__(self):
        super(HasInputCol, self).__init__()
        #: param for input column name
        self.inputCol = Param(self, "inputCol", "input column name")
        if None is not None:
            self._setDefault(inputCol=None)

    def setInputCol(self, value):
        """
        Sets the value of :py:attr:`inputCol`.
        """
        self.paramMap[self.inputCol] = value
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

    # a placeholder to make it appear in the generated doc
    inputCols = Param(Params._dummy(), "inputCols", "input column names")

    def __init__(self):
        super(HasInputCols, self).__init__()
        #: param for input column names
        self.inputCols = Param(self, "inputCols", "input column names")
        if None is not None:
            self._setDefault(inputCols=None)

    def setInputCols(self, value):
        """
        Sets the value of :py:attr:`inputCols`.
        """
        self.paramMap[self.inputCols] = value
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

    # a placeholder to make it appear in the generated doc
    outputCol = Param(Params._dummy(), "outputCol", "output column name")

    def __init__(self):
        super(HasOutputCol, self).__init__()
        #: param for output column name
        self.outputCol = Param(self, "outputCol", "output column name")
        if None is not None:
            self._setDefault(outputCol=None)

    def setOutputCol(self, value):
        """
        Sets the value of :py:attr:`outputCol`.
        """
        self.paramMap[self.outputCol] = value
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

    # a placeholder to make it appear in the generated doc
    numFeatures = Param(Params._dummy(), "numFeatures", "number of features")

    def __init__(self):
        super(HasNumFeatures, self).__init__()
        #: param for number of features
        self.numFeatures = Param(self, "numFeatures", "number of features")
        if None is not None:
            self._setDefault(numFeatures=None)

    def setNumFeatures(self, value):
        """
        Sets the value of :py:attr:`numFeatures`.
        """
        self.paramMap[self.numFeatures] = value
        return self

    def getNumFeatures(self):
        """
        Gets the value of numFeatures or its default value.
        """
        return self.getOrDefault(self.numFeatures)
