#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

# Stubs for pyspark.ml.recommendation (Python 3)
#

from typing import Any, Optional

import sys

from pyspark import since, keyword_only
from pyspark.ml.param.shared import (
    HasPredictionCol,
    HasBlockSize,
    HasMaxIter,
    HasRegParam,
    HasCheckpointInterval,
    HasSeed,
)
from pyspark.ml.wrapper import JavaEstimator, JavaModel
from pyspark.ml.common import inherit_doc
from pyspark.ml.param import Params, TypeConverters, Param
from pyspark.ml.util import JavaMLWritable, JavaMLReadable

from pyspark.sql.dataframe import DataFrame

class _ALSModelParams(HasPredictionCol):
    userCol: Param[str]
    itemCol: Param[str]
    coldStartStrategy: Param[str]
    def getUserCol(self) -> str: ...
    def getItemCol(self) -> str: ...
    def getColdStartStrategy(self) -> str: ...

class _ALSParams(
    _ALSModelParams, HasMaxIter, HasRegParam, HasCheckpointInterval, HasSeed
):
    rank: Param[int]
    numUserBlocks: Param[int]
    numItemBlocks: Param[int]
    implicitPrefs: Param[bool]
    alpha: Param[float]
    ratingCol: Param[str]
    nonnegative: Param[bool]
    intermediateStorageLevel: Param[str]
    finalStorageLevel: Param[str]
    def getRank(self) -> int: ...
    def getNumUserBlocks(self) -> int: ...
    def getNumItemBlocks(self) -> int: ...
    def getImplicitPrefs(self) -> bool: ...
    def getAlpha(self) -> float: ...
    def getRatingCol(self) -> str: ...
    def getNonnegative(self) -> bool: ...
    def getIntermediateStorageLevel(self) -> str: ...
    def getFinalStorageLevel(self) -> str: ...

class ALS(JavaEstimator[ALSModel], _ALSParams, JavaMLWritable, JavaMLReadable[ALS]):
    def __init__(
        self,
        *,
        rank: int = ...,
        maxIter: int = ...,
        regParam: float = ...,
        numUserBlocks: int = ...,
        numItemBlocks: int = ...,
        implicitPrefs: bool = ...,
        alpha: float = ...,
        userCol: str = ...,
        itemCol: str = ...,
        seed: Optional[int] = ...,
        ratingCol: str = ...,
        nonnegative: bool = ...,
        checkpointInterval: int = ...,
        intermediateStorageLevel: str = ...,
        finalStorageLevel: str = ...,
        coldStartStrategy: str = ...
    ) -> None: ...
    def setParams(
        self,
        *,
        rank: int = ...,
        maxIter: int = ...,
        regParam: float = ...,
        numUserBlocks: int = ...,
        numItemBlocks: int = ...,
        implicitPrefs: bool = ...,
        alpha: float = ...,
        userCol: str = ...,
        itemCol: str = ...,
        seed: Optional[int] = ...,
        ratingCol: str = ...,
        nonnegative: bool = ...,
        checkpointInterval: int = ...,
        intermediateStorageLevel: str = ...,
        finalStorageLevel: str = ...,
        coldStartStrategy: str = ...
    ) -> ALS: ...
    def setRank(self, value: int) -> ALS: ...
    def setNumUserBlocks(self, value: int) -> ALS: ...
    def setNumItemBlocks(self, value: int) -> ALS: ...
    def setNumBlocks(self, value: int) -> ALS: ...
    def setImplicitPrefs(self, value: bool) -> ALS: ...
    def setAlpha(self, value: float) -> ALS: ...
    def setUserCol(self, value: str) -> ALS: ...
    def setItemCol(self, value: str) -> ALS: ...
    def setRatingCol(self, value: str) -> ALS: ...
    def setNonnegative(self, value: bool) -> ALS: ...
    def setIntermediateStorageLevel(self, value: str) -> ALS: ...
    def setFinalStorageLevel(self, value: str) -> ALS: ...
    def setColdStartStrategy(self, value: str) -> ALS: ...
    def setMaxIter(self, value: int) -> ALS: ...
    def setRegParam(self, value: float) -> ALS: ...
    def setPredictionCol(self, value: str) -> ALS: ...
    def setCheckpointInterval(self, value: int) -> ALS: ...
    def setSeed(self, value: int) -> ALS: ...

class ALSModel(JavaModel, _ALSModelParams, JavaMLWritable, JavaMLReadable[ALSModel]):
    def setUserCol(self, value: str) -> ALSModel: ...
    def setItemCol(self, value: str) -> ALSModel: ...
    def setColdStartStrategy(self, value: str) -> ALSModel: ...
    def setPredictionCol(self, value: str) -> ALSModel: ...
    @property
    def rank(self) -> int: ...
    @property
    def userFactors(self) -> DataFrame: ...
    @property
    def itemFactors(self) -> DataFrame: ...
    def recommendForAllUsers(self, numItems: int) -> DataFrame: ...
    def recommendForAllItems(self, numUsers: int) -> DataFrame: ...
    def recommendForUserSubset(
        self, dataset: DataFrame, numItems: int
    ) -> DataFrame: ...
    def recommendForItemSubset(
        self, dataset: DataFrame, numUsers: int
    ) -> DataFrame: ...
