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

from typing import Any, Dict, Generic, Optional, Type, TypeVar, Union

from pyspark import SparkContext as SparkContext, since as since  # noqa: F401
from pyspark.ml.common import inherit_doc as inherit_doc  # noqa: F401
from pyspark.sql import SparkSession as SparkSession
from pyspark.util import VersionUtils as VersionUtils  # noqa: F401

S = TypeVar("S")
R = TypeVar("R", bound=MLReadable)

class Identifiable:
    uid: str
    def __init__(self) -> None: ...

class BaseReadWrite:
    def __init__(self) -> None: ...
    def session(self, sparkSession: SparkSession) -> Union[MLWriter, MLReader]: ...
    @property
    def sparkSession(self) -> SparkSession: ...
    @property
    def sc(self) -> SparkContext: ...

class MLWriter(BaseReadWrite):
    shouldOverwrite: bool = ...
    def __init__(self) -> None: ...
    def save(self, path: str) -> None: ...
    def saveImpl(self, path: str) -> None: ...
    def overwrite(self) -> MLWriter: ...

class GeneralMLWriter(MLWriter):
    source: str
    def format(self, source: str) -> MLWriter: ...

class JavaMLWriter(MLWriter):
    def __init__(self, instance: JavaMLWritable) -> None: ...
    def save(self, path: str) -> None: ...
    def overwrite(self) -> JavaMLWriter: ...
    def option(self, key: str, value: Any) -> JavaMLWriter: ...
    def session(self, sparkSession: SparkSession) -> JavaMLWriter: ...

class GeneralJavaMLWriter(JavaMLWriter):
    def __init__(self, instance: MLWritable) -> None: ...
    def format(self, source: str) -> GeneralJavaMLWriter: ...

class MLWritable:
    def write(self) -> MLWriter: ...
    def save(self, path: str) -> None: ...

class JavaMLWritable(MLWritable):
    def write(self) -> JavaMLWriter: ...

class GeneralJavaMLWritable(JavaMLWritable):
    def write(self) -> GeneralJavaMLWriter: ...

class MLReader(BaseReadWrite, Generic[R]):
    def load(self, path: str) -> R: ...

class JavaMLReader(MLReader[R]):
    def __init__(self, clazz: Type[JavaMLReadable]) -> None: ...
    def load(self, path: str) -> R: ...
    def session(self, sparkSession: SparkSession) -> JavaMLReader[R]: ...

class MLReadable(Generic[R]):
    @classmethod
    def read(cls: Type[R]) -> MLReader[R]: ...
    @classmethod
    def load(cls: Type[R], path: str) -> R: ...

class JavaMLReadable(MLReadable[R]):
    @classmethod
    def read(cls: Type[R]) -> JavaMLReader[R]: ...

class DefaultParamsWritable(MLWritable):
    def write(self) -> MLWriter: ...

class DefaultParamsWriter(MLWriter):
    instance: DefaultParamsWritable
    def __init__(self, instance: DefaultParamsWritable) -> None: ...
    def saveImpl(self, path: str) -> None: ...
    @staticmethod
    def saveMetadata(
        instance: DefaultParamsWritable,
        path: str,
        sc: SparkContext,
        extraMetadata: Optional[Dict[str, Any]] = ...,
        paramMap: Optional[Dict[str, Any]] = ...,
    ) -> None: ...

class DefaultParamsReadable(MLReadable[R]):
    @classmethod
    def read(cls: Type[R]) -> MLReader[R]: ...

class DefaultParamsReader(MLReader[R]):
    cls: Type[R]
    def __init__(self, cls: Type[MLReadable]) -> None: ...
    def load(self, path: str) -> R: ...
    @staticmethod
    def loadMetadata(
        path: str, sc: SparkContext, expectedClassName: str = ...
    ) -> Dict[str, Any]: ...
    @staticmethod
    def getAndSetParams(instance: R, metadata: Dict[str, Any]) -> None: ...
    @staticmethod
    def loadParamsInstance(path: str, sc: SparkContext) -> R: ...

class HasTrainingSummary(Generic[S]):
    @property
    def hasSummary(self) -> bool: ...
    @property
    def summary(self) -> S: ...

class MetaAlgorithmReadWrite:
    @staticmethod
    def isMetaEstimator(pyInstance: Any) -> bool: ...
    @staticmethod
    def getAllNestedPyAndJavaStages(pyInstance: Any, javaInstance) -> list: ...
    @staticmethod
    def meta_estimator_transfer_param_maps_to_java(
            pyEstimator: Any,
            javaEstimator: Any,
            pyParamMaps: list) -> list: ...
    @staticmethod
    def meta_estimator_transfer_param_maps_from_java(
            pyEstimator: Any,
            javaEstimator: Any,
            javaParamMaps: list) -> list: ...
