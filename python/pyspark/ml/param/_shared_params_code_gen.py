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

header = """#
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
#"""

# Code generator for shared params (shared.py). Run under this folder with:
# python _shared_params_code_gen.py > shared.py


def _gen_param_header(name, doc, defaultValueStr, typeConverter):
    """
    Generates the header part for shared variables

    :param name: param name
    :param doc: param doc
    """
    template = '''class Has$Name(Params):
    """
    Mixin for param $name: $doc
    """

    $name = Param(Params._dummy(), "$name", "$doc", typeConverter=$typeConverter)

    def __init__(self):
        super(Has$Name, self).__init__()'''

    if defaultValueStr is not None:
        template += '''
        self._setDefault($name=$defaultValueStr)'''

    Name = name[0].upper() + name[1:]
    if typeConverter is None:
        typeConverter = str(None)
    return template \
        .replace("$name", name) \
        .replace("$Name", Name) \
        .replace("$doc", doc) \
        .replace("$defaultValueStr", str(defaultValueStr)) \
        .replace("$typeConverter", typeConverter)


def _gen_param_code(name, doc, defaultValueStr):
    """
    Generates Python code for a shared param class.

    :param name: param name
    :param doc: param doc
    :param defaultValueStr: string representation of the default value
    :return: code string
    """
    # TODO: How to correctly inherit instance attributes?
    template = '''
    def get$Name(self):
        """
        Gets the value of $name or its default value.
        """
        return self.getOrDefault(self.$name)'''

    Name = name[0].upper() + name[1:]
    return template \
        .replace("$name", name) \
        .replace("$Name", Name) \
        .replace("$doc", doc) \
        .replace("$defaultValueStr", str(defaultValueStr))

if __name__ == "__main__":
    print(header)
    print("\n# DO NOT MODIFY THIS FILE! It was generated by _shared_params_code_gen.py.\n")
    print("from pyspark.ml.param import *\n\n")
    shared = [
        ("maxIter", "max number of iterations (>= 0).", None, "TypeConverters.toInt"),
        ("regParam", "regularization parameter (>= 0).", None, "TypeConverters.toFloat"),
        ("featuresCol", "features column name.", "'features'", "TypeConverters.toString"),
        ("labelCol", "label column name.", "'label'", "TypeConverters.toString"),
        ("predictionCol", "prediction column name.", "'prediction'", "TypeConverters.toString"),
        ("probabilityCol", "Column name for predicted class conditional probabilities. " +
         "Note: Not all models output well-calibrated probability estimates! These probabilities " +
         "should be treated as confidences, not precise probabilities.", "'probability'",
         "TypeConverters.toString"),
        ("rawPredictionCol", "raw prediction (a.k.a. confidence) column name.", "'rawPrediction'",
         "TypeConverters.toString"),
        ("inputCol", "input column name.", None, "TypeConverters.toString"),
        ("inputCols", "input column names.", None, "TypeConverters.toListString"),
        ("outputCol", "output column name.", "self.uid + '__output'", "TypeConverters.toString"),
        ("outputCols", "output column names.", None, "TypeConverters.toListString"),
        ("numFeatures", "Number of features. Should be greater than 0.", "262144",
         "TypeConverters.toInt"),
        ("checkpointInterval", "set checkpoint interval (>= 1) or disable checkpoint (-1). " +
         "E.g. 10 means that the cache will get checkpointed every 10 iterations. Note: " +
         "this setting will be ignored if the checkpoint directory is not set in the SparkContext.",
         None, "TypeConverters.toInt"),
        ("seed", "random seed.", "hash(type(self).__name__)", "TypeConverters.toInt"),
        ("tol", "the convergence tolerance for iterative algorithms (>= 0).", None,
         "TypeConverters.toFloat"),
        ("relativeError", "the relative target precision for the approximate quantile " +
         "algorithm. Must be in the range [0, 1]", "0.001", "TypeConverters.toFloat"),
        ("stepSize", "Step size to be used for each iteration of optimization (>= 0).", None,
         "TypeConverters.toFloat"),
        ("handleInvalid", "how to handle invalid entries. Options are skip (which will filter " +
         "out rows with bad values), or error (which will throw an error). More options may be " +
         "added later.", None, "TypeConverters.toString"),
        ("elasticNetParam", "the ElasticNet mixing parameter, in range [0, 1]. For alpha = 0, " +
         "the penalty is an L2 penalty. For alpha = 1, it is an L1 penalty.", "0.0",
         "TypeConverters.toFloat"),
        ("fitIntercept", "whether to fit an intercept term.", "True", "TypeConverters.toBoolean"),
        ("standardization", "whether to standardize the training features before fitting the " +
         "model.", "True", "TypeConverters.toBoolean"),
        ("thresholds", "Thresholds in multi-class classification to adjust the probability of " +
         "predicting each class. Array must have length equal to the number of classes, with " +
         "values > 0, excepting that at most one value may be 0. " +
         "The class with largest value p/t is predicted, where p is the original " +
         "probability of that class and t is the class's threshold.", None,
         "TypeConverters.toListFloat"),
        ("threshold", "threshold in binary classification prediction, in range [0, 1]",
         "0.5", "TypeConverters.toFloat"),
        ("weightCol", "weight column name. If this is not set or empty, we treat " +
         "all instance weights as 1.0.", None, "TypeConverters.toString"),
        ("solver", "the solver algorithm for optimization. If this is not set or empty, " +
         "default value is 'auto'.", "'auto'", "TypeConverters.toString"),
        ("varianceCol", "column name for the biased sample variance of prediction.",
         None, "TypeConverters.toString"),
        ("aggregationDepth", "suggested depth for treeAggregate (>= 2).", "2",
         "TypeConverters.toInt"),
        ("parallelism", "the number of threads to use when running parallel algorithms (>= 1).",
         "1", "TypeConverters.toInt"),
        ("collectSubModels", "Param for whether to collect a list of sub-models trained during " +
         "tuning. If set to false, then only the single best sub-model will be available after " +
         "fitting. If set to true, then all sub-models will be available. Warning: For large " +
         "models, collecting all sub-models can cause OOMs on the Spark driver.",
         "False", "TypeConverters.toBoolean"),
        ("loss", "the loss function to be optimized.", None, "TypeConverters.toString"),
        ("distanceMeasure", "the distance measure. Supported options: 'euclidean' and 'cosine'.",
         "'euclidean'", "TypeConverters.toString"),
        ("validationIndicatorCol", "name of the column that indicates whether each row is for " +
         "training or for validation. False indicates training; true indicates validation.",
         None, "TypeConverters.toString"),
        ("blockSize", "block size for stacking input data in matrices. Data is stacked within "
         "partitions. If block size is more than remaining data in a partition then it is "
         "adjusted to the size of this data.", None, "TypeConverters.toInt"),
        ("maxBlockSizeInMB", "maximum memory in MB for stacking input data into blocks. Data is " +
         "stacked within partitions. If more than remaining data size in a partition then it " +
         "is adjusted to the data size. Default 0.0 represents choosing optimal value, depends " +
         "on specific algorithm. Must be >= 0.", "0.0", "TypeConverters.toFloat"),
        ("intermediateStorageLevel", "storageLevel for intermediate datasets. Cannot be NONE.",
         "'MEMORY_AND_DISK'", "TypeConverters.toString")]

    code = []
    for name, doc, defaultValueStr, typeConverter in shared:
        param_code = _gen_param_header(name, doc, defaultValueStr, typeConverter)
        code.append(param_code + "\n" + _gen_param_code(name, doc, defaultValueStr))

    print("\n\n\n".join(code))
