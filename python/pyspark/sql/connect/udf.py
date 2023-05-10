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
"""
User-defined function related classes and functions
"""
from pyspark.sql.connect.utils import check_dependencies

check_dependencies(__name__)

import sys
import functools
import warnings
from inspect import getfullargspec
from typing import cast, Callable, Any, TYPE_CHECKING, Optional, Union

from pyspark.rdd import PythonEvalType
from pyspark.sql.connect.expressions import (
    ColumnReference,
    PythonUDF,
    CommonInlineUserDefinedFunction,
)
from pyspark.sql.connect.column import Column
from pyspark.sql.connect.types import UnparsedDataType
from pyspark.sql.types import ArrayType, DataType, MapType, StringType, StructType
from pyspark.sql.udf import UDFRegistration as PySparkUDFRegistration, _create_arrow_py_udf
from pyspark.errors import PySparkTypeError


if TYPE_CHECKING:
    from pyspark.sql.connect._typing import (
        ColumnOrName,
        DataTypeOrString,
        UserDefinedFunctionLike,
    )
    from pyspark.sql.connect.session import SparkSession
    from pyspark.sql.types import StringType


def _create_py_udf(
    f: Callable[..., Any],
    returnType: "DataTypeOrString",
    useArrow: Optional[bool] = None,
) -> "UserDefinedFunctionLike":
    from pyspark.sql.connect.session import _active_spark_session

    if _active_spark_session is None:
        is_arrow_enabled = False
    else:
        is_arrow_enabled = (
            _active_spark_session.conf.get("spark.sql.execution.pythonUDF.arrow.enabled") == "true"
            if useArrow is None
            else useArrow
        )

    regular_udf = _create_udf(f, returnType, PythonEvalType.SQL_BATCHED_UDF)
    return_type = regular_udf.returnType
    try:
        is_func_with_args = len(getfullargspec(f).args) > 0
    except TypeError:
        is_func_with_args = False
    is_output_atomic_type = (
        not isinstance(return_type, StructType)
        and not isinstance(return_type, MapType)
        and not isinstance(return_type, ArrayType)
    )
    if is_arrow_enabled:
        if is_output_atomic_type and is_func_with_args:
            return _create_arrow_py_udf(regular_udf)
        else:
            warnings.warn(
                "Arrow optimization for Python UDFs cannot be enabled.",
                UserWarning,
            )
            return regular_udf
    else:
        return regular_udf


def _create_udf(
    f: Callable[..., Any],
    returnType: "DataTypeOrString",
    evalType: int,
    name: Optional[str] = None,
    deterministic: bool = True,
) -> "UserDefinedFunctionLike":
    # Set the name of the UserDefinedFunction object to be the name of function f
    udf_obj = UserDefinedFunction(
        f, returnType=returnType, name=name, evalType=evalType, deterministic=deterministic
    )
    return udf_obj._wrapped()


class UserDefinedFunction:
    """
    User defined function in Python

    Notes
    -----
    The constructor of this class is not supposed to be directly called.
    Use :meth:`pyspark.sql.functions.udf` or :meth:`pyspark.sql.functions.pandas_udf`
    to create this instance.
    """

    def __init__(
        self,
        func: Callable[..., Any],
        returnType: "DataTypeOrString" = StringType(),
        name: Optional[str] = None,
        evalType: int = PythonEvalType.SQL_BATCHED_UDF,
        deterministic: bool = True,
    ):
        if not callable(func):
            raise PySparkTypeError(
                error_class="NOT_CALLABLE",
                message_parameters={"arg_name": "func", "arg_type": type(func).__name__},
            )

        if not isinstance(returnType, (DataType, str)):
            raise PySparkTypeError(
                error_class="NOT_DATATYPE_OR_STR",
                message_parameters={
                    "arg_name": "returnType",
                    "arg_type": type(returnType).__name__,
                },
            )

        if not isinstance(evalType, int):
            raise PySparkTypeError(
                error_class="NOT_INT",
                message_parameters={"arg_name": "evalType", "arg_type": type(evalType).__name__},
            )

        self.func = func
        self.returnType: DataType = (
            UnparsedDataType(returnType) if isinstance(returnType, str) else returnType
        )
        self._name = name or (
            func.__name__ if hasattr(func, "__name__") else func.__class__.__name__
        )
        self.evalType = evalType
        self.deterministic = deterministic

    def _build_common_inline_user_defined_function(
        self, *cols: "ColumnOrName"
    ) -> CommonInlineUserDefinedFunction:
        arg_cols = [
            col if isinstance(col, Column) else Column(ColumnReference(col)) for col in cols
        ]
        arg_exprs = [col._expr for col in arg_cols]

        py_udf = PythonUDF(
            output_type=self.returnType,
            eval_type=self.evalType,
            func=self.func,
            python_ver="%d.%d" % sys.version_info[:2],
        )
        return CommonInlineUserDefinedFunction(
            function_name=self._name,
            function=py_udf,
            deterministic=self.deterministic,
            arguments=arg_exprs,
        )

    def __call__(self, *cols: "ColumnOrName") -> Column:
        return Column(self._build_common_inline_user_defined_function(*cols))

    # This function is for improving the online help system in the interactive interpreter.
    # For example, the built-in help / pydoc.help. It wraps the UDF with the docstring and
    # argument annotation. (See: SPARK-19161)
    def _wrapped(self) -> "UserDefinedFunctionLike":
        """
        Wrap this udf with a function and attach docstring from func
        """

        # It is possible for a callable instance without __name__ attribute or/and
        # __module__ attribute to be wrapped here. For example, functools.partial. In this case,
        # we should avoid wrapping the attributes from the wrapped function to the wrapper
        # function. So, we take out these attribute names from the default names to set and
        # then manually assign it after being wrapped.
        assignments = tuple(
            a for a in functools.WRAPPER_ASSIGNMENTS if a != "__name__" and a != "__module__"
        )

        @functools.wraps(self.func, assigned=assignments)
        def wrapper(*args: "ColumnOrName") -> Column:
            return self(*args)

        wrapper.__name__ = self._name
        wrapper.__module__ = (
            self.func.__module__
            if hasattr(self.func, "__module__")
            else self.func.__class__.__module__
        )

        wrapper.func = self.func  # type: ignore[attr-defined]
        wrapper.returnType = self.returnType  # type: ignore[attr-defined]
        wrapper.evalType = self.evalType  # type: ignore[attr-defined]
        wrapper.deterministic = self.deterministic  # type: ignore[attr-defined]
        wrapper.asNondeterministic = functools.wraps(  # type: ignore[attr-defined]
            self.asNondeterministic
        )(lambda: self.asNondeterministic()._wrapped())
        wrapper._unwrapped = self  # type: ignore[attr-defined]
        return wrapper  # type: ignore[return-value]

    def asNondeterministic(self) -> "UserDefinedFunction":
        """
        Updates UserDefinedFunction to nondeterministic.

        .. versionadded:: 3.4.0
        """
        self.deterministic = False
        return self


class UDFRegistration:
    """
    Wrapper for user-defined function registration.
    """

    def __init__(self, sparkSession: "SparkSession"):
        self.sparkSession = sparkSession

    def register(
        self,
        name: str,
        f: Union[Callable[..., Any], "UserDefinedFunctionLike"],
        returnType: Optional["DataTypeOrString"] = None,
    ) -> "UserDefinedFunctionLike":
        # This is to check whether the input function is from a user-defined function or
        # Python function.
        if hasattr(f, "asNondeterministic"):
            if returnType is not None:
                raise PySparkTypeError(
                    error_class="CANNOT_SPECIFY_RETURN_TYPE_FOR_UDF",
                    message_parameters={"arg_name": "f", "return_type": str(returnType)},
                )
            f = cast("UserDefinedFunctionLike", f)
            if f.evalType not in [
                PythonEvalType.SQL_BATCHED_UDF,
                PythonEvalType.SQL_ARROW_BATCHED_UDF,
                PythonEvalType.SQL_SCALAR_PANDAS_UDF,
                PythonEvalType.SQL_SCALAR_PANDAS_ITER_UDF,
                PythonEvalType.SQL_GROUPED_AGG_PANDAS_UDF,
            ]:
                raise PySparkTypeError(
                    error_class="INVALID_UDF_EVAL_TYPE",
                    message_parameters={
                        "eval_type": "SQL_BATCHED_UDF, SQL_ARROW_BATCHED_UDF, "
                        "SQL_SCALAR_PANDAS_UDF, SQL_SCALAR_PANDAS_ITER_UDF or "
                        "SQL_GROUPED_AGG_PANDAS_UDF"
                    },
                )
            source_udf = _create_udf(
                f.func,
                returnType=f.returnType,
                name=name,
                evalType=f.evalType,
                deterministic=f.deterministic,
            )
            if f.evalType == PythonEvalType.SQL_ARROW_BATCHED_UDF:
                register_udf = _create_arrow_py_udf(source_udf)._unwrapped
            else:
                register_udf = source_udf._unwrapped
            self.sparkSession._client.register_udf(
                register_udf.func, f.returnType, name, f.evalType, f.deterministic
            )
            return_udf = f
        else:
            if returnType is None:
                returnType = StringType()
            return_udf = _create_udf(
                f, returnType=returnType, evalType=PythonEvalType.SQL_BATCHED_UDF, name=name
            )

            self.sparkSession._client.register_udf(return_udf.func, returnType, name)

        return return_udf

    register.__doc__ = PySparkUDFRegistration.register.__doc__

    def registerJavaFunction(
        self,
        name: str,
        javaClassName: str,
        returnType: Optional["DataTypeOrString"] = None,
    ) -> None:
        self.sparkSession._client.register_java(name, javaClassName, returnType)

    registerJavaFunction.__doc__ = PySparkUDFRegistration.registerJavaFunction.__doc__

    def registerJavaUDAF(self, name: str, javaClassName: str) -> None:
        self.sparkSession._client.register_java(name, javaClassName, aggregate=True)

    registerJavaUDAF.__doc__ = PySparkUDFRegistration.registerJavaUDAF.__doc__
