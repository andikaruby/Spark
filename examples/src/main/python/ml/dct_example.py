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

# $example on$
from pyspark.ml.feature import DCT
from pyspark.mllib.linalg import Vectors
# $example off$
from pyspark.sql import SparkSession

if __name__ == "__main__":
    spark = SparkSession.builder.appName("DCTExample").getOrCreate()

    # $example on$
    df = spark.createDataFrame([
        (Vectors.dense([0.0, 1.0, -2.0, 3.0]),),
        (Vectors.dense([-1.0, 2.0, 4.0, -7.0]),),
        (Vectors.dense([14.0, -2.0, -5.0, 1.0]),)], ["features"])

    dct = DCT(inverse=False, inputCol="features", outputCol="featuresDCT")

    dctDf = dct.transform(df)

    for dcts in dctDf.select("featuresDCT").take(3):
        print(dcts)
    # $example off$

    spark.stop()
