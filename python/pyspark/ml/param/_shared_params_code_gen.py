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

from __future__ import print_function

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


def _gen_param_header(paramTypeStr, name, doc, defaultValueStr, isValidFunctionStr):
    """
    Generates the header part for shared variables

    :param name: param name
    :param doc: param doc
    """
    template = '''class Has$Name(Params):
    """
    Mixin for param $name: $doc
    """

    $name = $paramType(Params._dummy(), "$name", "$doc", $isValid)

    def __init__(self):
        super(Has$Name, self).__init__()'''

    if defaultValueStr is not None:
        template += '''
        self._setDefault($name=$defaultValueStr)'''

    Name = name[0].upper() + name[1:]
    # expectedTypeName = str(expectedType)
    # if expectedType is not None:
    #     expectedTypeName = expectedType.__name__
    if isValidFunctionStr is None:
        isValidFunctionStr = str(None)
    return template \
        .replace("$paramType", paramTypeStr) \
        .replace("$name", name) \
        .replace("$Name", Name) \
        .replace("$doc", doc) \
        .replace("$defaultValueStr", str(defaultValueStr)) \
        .replace("$isValid", isValidFunctionStr)


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
    def set$Name(self, value):
        """
        Sets the value of :py:attr:`$name`.
        """
        self._set($name=value)
        return self

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
        ("IntParam", "maxIter", "max number of iterations (>= 0).", None, "ParamValidators.gtEq(0)"),
        ("FloatParam", "regParam", "regularization parameter (>= 0).", None, "ParamValidators.gtEq(0)"),
        ("StringParam", "featuresCol", "features column name.", "'features'", None),
        ("StringParam", "labelCol", "label column name.", "'label'", None),
        ("StringParam", "predictionCol", "prediction column name.", "'prediction'", None),
        ("StringParam", "probabilityCol", "Column name for predicted class conditional probabilities. " +
         "Note: Not all models output well-calibrated probability estimates! These probabilities " +
         "should be treated as confidences, not precise probabilities.", "'probability'", None),
        ("StringParam", "rawPredictionCol", "raw prediction (a.k.a. confidence) column name.", "'rawPrediction'", None),
        ("StringParam", "inputCol", "input column name.", None, None),
        ("ListStringParam", "inputCols", "input column names.", None, None),
        ("StringParam", "outputCol", "output column name.", "self.uid + '__output'", None),
        ("IntParam", "numFeatures", "number of features.", None, "ParamValidators.gtEq(0)"),
        ("IntParam", "checkpointInterval", "set checkpoint interval (>= 1) or disable checkpoint (-1). " +
         "E.g. 10 means that the cache will get checkpointed every 10 iterations.", None,
         "lambda interval: (interval == -1) or (interval >= 1)"),
        ("IntParam", "seed", "random seed.", "hash(type(self).__name__)", None),
        ("BooleanParam", "tol", "the convergence tolerance for iterative algorithms.", None, None),
        ("FloatParam", "stepSize", "Step size to be used for each iteration of optimization.", None, None),
        ("StringParam", "handleInvalid", "how to handle invalid entries. Options are skip (which will filter " +
         "out rows with bad values), or error (which will throw an errror). More options may be " +
         "added later.", None, "ParamValidators.inList(['skip', 'error'])"),
        ("FloatParam", "elasticNetParam", "the ElasticNet mixing parameter, in range [0, 1]. For alpha = 0, " +
         "the penalty is an L2 penalty. For alpha = 1, it is an L1 penalty.", "0.0",
         "ParamValidators.inRange(0, 1)"),
        ("BooleanParam", "fitIntercept", "whether to fit an intercept term.", "True", None),
        ("BooleanParam", "standardization", "whether to standardize the training features before fitting the " +
         "model.", "True", None),
        ("ListFloatParam", "thresholds", "Thresholds in multi-class classification to adjust the probability of " +
         "predicting each class. Array must have length equal to the number of classes, with " +
         "values >= 0. The class with largest value p/t is predicted, where p is the original " +
         "probability of that class and t is the class' threshold.", None,
         "lambda lst: all(map(lambda t: t >= 0, lst))"),
        ("StringParam", "weightCol", "weight column name. If this is not set or empty, we treat " +
         "all instance weights as 1.0.", None, None),
        ("StringParam", "solver", "the solver algorithm for optimization. If this is not set or empty, " +
         "default value is 'auto'.", "'auto'", None)]

    code = []
    for paramClassStr, name, doc, defaultValueStr, isValidFunctionStr in shared:
        param_code = _gen_param_header(paramClassStr, name, doc, defaultValueStr, isValidFunctionStr)
        code.append(param_code + "\n" + _gen_param_code(name, doc, defaultValueStr))

    decisionTreeParams = [
        ("maxDepth", "Maximum depth of the tree. (>= 0) E.g., depth 0 means 1 leaf node; " +
         "depth 1 means 1 internal node + 2 leaf nodes."),
        ("maxBins", "Max number of bins for" +
         " discretizing continuous features.  Must be >=2 and >= number of categories for any" +
         " categorical feature."),
        ("minInstancesPerNode", "Minimum number of instances each child must have after split. " +
         "If a split causes the left or right child to have fewer than minInstancesPerNode, the " +
         "split will be discarded as invalid. Should be >= 1."),
        ("minInfoGain", "Minimum information gain for a split to be considered at a tree node."),
        ("maxMemoryInMB", "Maximum memory in MB allocated to histogram aggregation."),
        ("cacheNodeIds", "If false, the algorithm will pass trees to executors to match " +
         "instances with nodes. If true, the algorithm will cache node IDs for each instance. " +
         "Caching can speed up training of deeper trees. Users can set how often should the " +
         "cache be checkpointed or disable it by setting checkpointInterval.")]

    decisionTreeCode = '''class DecisionTreeParams(Params):
    """
    Mixin for Decision Tree parameters.
    """

    $dummyPlaceHolders

    def __init__(self):
        super(DecisionTreeParams, self).__init__()'''
    dtParamMethods = ""
    dummyPlaceholders = ""
    paramTemplate = """$name = Param($owner, "$name", "$doc")"""
    for name, doc in decisionTreeParams:
        variable = paramTemplate.replace("$name", name).replace("$doc", doc)
        dummyPlaceholders += variable.replace("$owner", "Params._dummy()") + "\n    "
        dtParamMethods += _gen_param_code(name, doc, None) + "\n"
    code.append(decisionTreeCode.replace("$dummyPlaceHolders", dummyPlaceholders) + "\n" +
                dtParamMethods)
    print("\n\n\n".join(code))
