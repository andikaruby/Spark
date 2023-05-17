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

import numpy as np
import pandas as pd

from pyspark.sql.functions import col, pandas_udf

from pyspark.mlv2.base import Estimator, Model, Transformer
from pyspark.mlv2.util import transform_dataframe_column
from pyspark.mlv2.summarizer import summarize_dataframe
from pyspark.ml.param.shared import HasInputCol, HasOutputCol
from pyspark.ml.functions import array_to_vector


class MaxAbsScaler(Estimator, HasInputCol, HasOutputCol):
    """
    Rescale each feature individually to range [-1, 1] by dividing through the largest maximum
    absolute value in each feature. It does not shift/center the data, and thus does not destroy
    any sparsity.
    """

    def _fit(self, dataset):
        input_col = self.getInputCol()

        dataset = dataset.withColumn(input_col, array_to_vector(col(input_col)))

        min_max_res = summarize_dataframe(dataset, input_col, ["min", "max"])
        min_values = min_max_res["min"]
        max_values = min_max_res["max"]

        max_abs_values = np.maximum(np.abs(min_values), np.abs(max_values))

        return self._copyValues(MaxAbsScalerModel(max_abs_values))


class MaxAbsScalerModel(Transformer, HasInputCol, HasOutputCol):

    def __init__(self, max_abs_values):
        self.max_abs_values = max_abs_values

    def transform(self, dataset):

        input_col = self.getInputCol()
        output_col = self.getOutputCol()

        max_abs_values = self.max_abs_values
        max_abs_values_zero_cond = (max_abs_values == 0.0)

        def transform_pandas_series(series):
            def map_value(x):
                return max_abs_values.where(max_abs_values_zero_cond, 0.0, x / max_abs_values)

            return series.apply(map_value)

        return transform_dataframe_column(
            dataset,
            input_col_name=input_col,
            transform_fn=transform_pandas_series,
            result_col_name=output_col,
            result_col_spark_type='double'
        )
