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

from typing import Any, Callable, Dict, Optional, cast

import py4j
from py4j.protocol import Py4JJavaError
from py4j.java_gateway import is_instance_of

from pyspark import SparkContext
from pyspark.errors.exceptions.base import (
    AnalysisException,
    IllegalArgumentException,
    ParseException,
    PySparkException,
    PythonException,
    QueryExecutionException,
    SparkUpgradeException,
    StreamingQueryException,
    UnknownException,
)


class CapturedException(PySparkException):
    def __init__(
        self,
        desc: Optional[str] = None,
        stackTrace: Optional[str] = None,
        cause: Optional[Py4JJavaError] = None,
        origin: Optional[Py4JJavaError] = None,
    ):
        # desc & stackTrace vs origin are mutually exclusive.
        # cause is optional.
        assert (origin is not None and desc is None and stackTrace is None) or (
            origin is None and desc is not None and stackTrace is not None
        )

        self.desc = desc if desc is not None else cast(Py4JJavaError, origin).getMessage()
        assert SparkContext._jvm is not None
        self.stackTrace = (
            stackTrace
            if stackTrace is not None
            else (SparkContext._jvm.org.apache.spark.util.Utils.exceptionString(origin))
        )
        self.cause = convert_exception(cause) if cause is not None else None
        if self.cause is None and origin is not None and origin.getCause() is not None:
            self.cause = convert_exception(origin.getCause())
        self._origin = origin

    def __str__(self) -> str:
        assert SparkContext._jvm is not None

        jvm = SparkContext._jvm
        sql_conf = jvm.org.apache.spark.sql.internal.SQLConf.get()
        debug_enabled = sql_conf.pysparkJVMStacktraceEnabled()
        desc = self.desc
        if debug_enabled:
            desc = desc + "\n\nJVM stacktrace:\n%s" % self.stackTrace
        return str(desc)

    def getErrorClass(self) -> Optional[str]:
        assert SparkContext._gateway is not None

        gw = SparkContext._gateway
        if self._origin is not None and is_instance_of(
            gw, self._origin, "org.apache.spark.SparkThrowable"
        ):
            return self._origin.getErrorClass()
        else:
            return None

    def getMessageParameters(self) -> Optional[Dict[str, str]]:
        assert SparkContext._gateway is not None

        gw = SparkContext._gateway
        if self._origin is not None and is_instance_of(
            gw, self._origin, "org.apache.spark.SparkThrowable"
        ):
            return self._origin.getMessageParameters()
        else:
            return None

    def getSqlState(self) -> Optional[str]:  # type: ignore[override]
        assert SparkContext._gateway is not None
        gw = SparkContext._gateway
        if self._origin is not None and is_instance_of(
            gw, self._origin, "org.apache.spark.SparkThrowable"
        ):
            return self._origin.getSqlState()
        else:
            return None


def convert_exception(e: Py4JJavaError) -> CapturedException:
    assert e is not None
    assert SparkContext._jvm is not None
    assert SparkContext._gateway is not None

    jvm = SparkContext._jvm
    gw = SparkContext._gateway

    if is_instance_of(gw, e, "org.apache.spark.sql.catalyst.parser.ParseException"):
        return CapturedParseException(origin=e)
    # Order matters. ParseException inherits AnalysisException.
    elif is_instance_of(gw, e, "org.apache.spark.sql.AnalysisException"):
        return CapturedAnalysisException(origin=e)
    elif is_instance_of(gw, e, "org.apache.spark.sql.streaming.StreamingQueryException"):
        return CapturedStreamingQueryException(origin=e)
    elif is_instance_of(gw, e, "org.apache.spark.sql.execution.QueryExecutionException"):
        return CapturedQueryExecutionException(origin=e)
    elif is_instance_of(gw, e, "java.lang.IllegalArgumentException"):
        return CapturedIllegalArgumentException(origin=e)
    elif is_instance_of(gw, e, "org.apache.spark.SparkUpgradeException"):
        return CapturedSparkUpgradeException(origin=e)

    c: Py4JJavaError = e.getCause()
    stacktrace: str = jvm.org.apache.spark.util.Utils.exceptionString(e)
    if c is not None and (
        is_instance_of(gw, c, "org.apache.spark.api.python.PythonException")
        # To make sure this only catches Python UDFs.
        and any(
            map(
                lambda v: "org.apache.spark.sql.execution.python" in v.toString(), c.getStackTrace()
            )
        )
    ):
        msg = (
            "\n  An exception was thrown from the Python worker. "
            "Please see the stack trace below.\n%s" % c.getMessage()
        )
        return CapturedPythonException(msg, stacktrace)

    return CapturedUnknownException(desc=e.toString(), stackTrace=stacktrace, cause=c)


def capture_sql_exception(f: Callable[..., Any]) -> Callable[..., Any]:
    def deco(*a: Any, **kw: Any) -> Any:
        try:
            return f(*a, **kw)
        except Py4JJavaError as e:
            converted = convert_exception(e.java_exception)
            if not isinstance(converted, CapturedUnknownException):
                # Hide where the exception came from that shows a non-Pythonic
                # JVM exception message.
                raise converted from None
            else:
                raise

    return deco


def install_exception_handler() -> None:
    """
    Hook an exception handler into Py4j, which could capture some SQL exceptions in Java.

    When calling Java API, it will call `get_return_value` to parse the returned object.
    If any exception happened in JVM, the result will be Java exception object, it raise
    py4j.protocol.Py4JJavaError. We replace the original `get_return_value` with one that
    could capture the Java exception and throw a Python one (with the same error message).

    It's idempotent, could be called multiple times.
    """
    original = py4j.protocol.get_return_value
    # The original `get_return_value` is not patched, it's idempotent.
    patched = capture_sql_exception(original)
    # only patch the one used in py4j.java_gateway (call Java API)
    py4j.java_gateway.get_return_value = patched


class CapturedAnalysisException(CapturedException, AnalysisException):
    """
    Failed to analyze a SQL query plan.
    """


class CapturedParseException(CapturedException, ParseException):
    """
    Failed to parse a SQL command.
    """


class CapturedIllegalArgumentException(CapturedException, IllegalArgumentException):
    """
    Passed an illegal or inappropriate argument.
    """


class CapturedStreamingQueryException(CapturedException, StreamingQueryException):
    """
    Exception that stopped a :class:`StreamingQuery`.
    """


class CapturedQueryExecutionException(CapturedException, QueryExecutionException):
    """
    Failed to execute a query.
    """


class CapturedPythonException(CapturedException, PythonException):
    """
    Exceptions thrown from Python workers.
    """


class CapturedUnknownException(CapturedException, UnknownException):
    """
    None of the above exceptions.
    """


class CapturedSparkUpgradeException(CapturedException, SparkUpgradeException):
    """
    Exception thrown because of Spark upgrade.
    """
