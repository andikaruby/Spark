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
@generated by mypy-protobuf.  Do not edit manually!
isort:skip_file

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
"""
import builtins
import collections.abc
import google.protobuf.any_pb2
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.internal.enum_type_wrapper
import google.protobuf.message
import pyspark.sql.connect.proto.commands_pb2
import pyspark.sql.connect.proto.relations_pb2
import pyspark.sql.connect.proto.types_pb2
import sys
import typing

if sys.version_info >= (3, 10):
    import typing as typing_extensions
else:
    import typing_extensions

DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

@typing_extensions.final
class Plan(google.protobuf.message.Message):
    """A [[Plan]] is the structure that carries the runtime information for the execution from the
    client to the server. A [[Plan]] can either be of the type [[Relation]] which is a reference
    to the underlying logical plan or it can be of the [[Command]] type that is used to execute
    commands on the server.
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    ROOT_FIELD_NUMBER: builtins.int
    COMMAND_FIELD_NUMBER: builtins.int
    @property
    def root(self) -> pyspark.sql.connect.proto.relations_pb2.Relation: ...
    @property
    def command(self) -> pyspark.sql.connect.proto.commands_pb2.Command: ...
    def __init__(
        self,
        *,
        root: pyspark.sql.connect.proto.relations_pb2.Relation | None = ...,
        command: pyspark.sql.connect.proto.commands_pb2.Command | None = ...,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions.Literal[
            "command", b"command", "op_type", b"op_type", "root", b"root"
        ],
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "command", b"command", "op_type", b"op_type", "root", b"root"
        ],
    ) -> None: ...
    def WhichOneof(
        self, oneof_group: typing_extensions.Literal["op_type", b"op_type"]
    ) -> typing_extensions.Literal["root", "command"] | None: ...

global___Plan = Plan

@typing_extensions.final
class Explain(google.protobuf.message.Message):
    """Explains the input plan based on a configurable mode."""

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    class _ExplainMode:
        ValueType = typing.NewType("ValueType", builtins.int)
        V: typing_extensions.TypeAlias = ValueType

    class _ExplainModeEnumTypeWrapper(
        google.protobuf.internal.enum_type_wrapper._EnumTypeWrapper[Explain._ExplainMode.ValueType],
        builtins.type,
    ):  # noqa: F821
        DESCRIPTOR: google.protobuf.descriptor.EnumDescriptor
        MODE_UNSPECIFIED: Explain._ExplainMode.ValueType  # 0
        SIMPLE: Explain._ExplainMode.ValueType  # 1
        """Generates only physical plan."""
        EXTENDED: Explain._ExplainMode.ValueType  # 2
        """Generates parsed logical plan, analyzed logical plan, optimized logical plan and physical plan.
        Parsed Logical plan is a unresolved plan that extracted from the query. Analyzed logical plans
        transforms which translates unresolvedAttribute and unresolvedRelation into fully typed objects.
        The optimized logical plan transforms through a set of optimization rules, resulting in the
        physical plan.
        """
        CODEGEN: Explain._ExplainMode.ValueType  # 3
        """Generates code for the statement, if any and a physical plan."""
        COST: Explain._ExplainMode.ValueType  # 4
        """If plan node statistics are available, generates a logical plan and also the statistics."""
        FORMATTED: Explain._ExplainMode.ValueType  # 5
        """Generates a physical plan outline and also node details."""

    class ExplainMode(_ExplainMode, metaclass=_ExplainModeEnumTypeWrapper):
        """Plan explanation mode."""

    MODE_UNSPECIFIED: Explain.ExplainMode.ValueType  # 0
    SIMPLE: Explain.ExplainMode.ValueType  # 1
    """Generates only physical plan."""
    EXTENDED: Explain.ExplainMode.ValueType  # 2
    """Generates parsed logical plan, analyzed logical plan, optimized logical plan and physical plan.
    Parsed Logical plan is a unresolved plan that extracted from the query. Analyzed logical plans
    transforms which translates unresolvedAttribute and unresolvedRelation into fully typed objects.
    The optimized logical plan transforms through a set of optimization rules, resulting in the
    physical plan.
    """
    CODEGEN: Explain.ExplainMode.ValueType  # 3
    """Generates code for the statement, if any and a physical plan."""
    COST: Explain.ExplainMode.ValueType  # 4
    """If plan node statistics are available, generates a logical plan and also the statistics."""
    FORMATTED: Explain.ExplainMode.ValueType  # 5
    """Generates a physical plan outline and also node details."""

    EXPLAIN_MODE_FIELD_NUMBER: builtins.int
    explain_mode: global___Explain.ExplainMode.ValueType
    """(Required) For analyzePlan rpc calls, configure the mode to explain plan in strings."""
    def __init__(
        self,
        *,
        explain_mode: global___Explain.ExplainMode.ValueType = ...,
    ) -> None: ...
    def ClearField(
        self, field_name: typing_extensions.Literal["explain_mode", b"explain_mode"]
    ) -> None: ...

global___Explain = Explain

@typing_extensions.final
class UserContext(google.protobuf.message.Message):
    """User Context is used to refer to one particular user session that is executing
    queries in the backend.
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    USER_ID_FIELD_NUMBER: builtins.int
    USER_NAME_FIELD_NUMBER: builtins.int
    EXTENSIONS_FIELD_NUMBER: builtins.int
    user_id: builtins.str
    user_name: builtins.str
    @property
    def extensions(
        self,
    ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
        google.protobuf.any_pb2.Any
    ]:
        """To extend the existing user context message that is used to identify incoming requests,
        Spark Connect leverages the Any protobuf type that can be used to inject arbitrary other
        messages into this message. Extensions are stored as a `repeated` type to be able to
        handle multiple active extensions.
        """
    def __init__(
        self,
        *,
        user_id: builtins.str = ...,
        user_name: builtins.str = ...,
        extensions: collections.abc.Iterable[google.protobuf.any_pb2.Any] | None = ...,
    ) -> None: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "extensions", b"extensions", "user_id", b"user_id", "user_name", b"user_name"
        ],
    ) -> None: ...

global___UserContext = UserContext

@typing_extensions.final
class AnalyzePlanRequest(google.protobuf.message.Message):
    """Request to perform plan analyze, optionally to explain the plan."""

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    CLIENT_ID_FIELD_NUMBER: builtins.int
    USER_CONTEXT_FIELD_NUMBER: builtins.int
    PLAN_FIELD_NUMBER: builtins.int
    CLIENT_TYPE_FIELD_NUMBER: builtins.int
    EXPLAIN_FIELD_NUMBER: builtins.int
    client_id: builtins.str
    """(Required)

    The client_id is set by the client to be able to collate streaming responses from
    different queries.
    """
    @property
    def user_context(self) -> global___UserContext:
        """(Required) User context"""
    @property
    def plan(self) -> global___Plan:
        """(Required) The logical plan to be analyzed."""
    client_type: builtins.str
    """Provides optional information about the client sending the request. This field
    can be used for language or version specific information and is only intended for
    logging purposes and will not be interpreted by the server.
    """
    @property
    def explain(self) -> global___Explain:
        """(Optional) Get the explain string of the plan."""
    def __init__(
        self,
        *,
        client_id: builtins.str = ...,
        user_context: global___UserContext | None = ...,
        plan: global___Plan | None = ...,
        client_type: builtins.str | None = ...,
        explain: global___Explain | None = ...,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions.Literal[
            "_client_type",
            b"_client_type",
            "client_type",
            b"client_type",
            "explain",
            b"explain",
            "plan",
            b"plan",
            "user_context",
            b"user_context",
        ],
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "_client_type",
            b"_client_type",
            "client_id",
            b"client_id",
            "client_type",
            b"client_type",
            "explain",
            b"explain",
            "plan",
            b"plan",
            "user_context",
            b"user_context",
        ],
    ) -> None: ...
    def WhichOneof(
        self, oneof_group: typing_extensions.Literal["_client_type", b"_client_type"]
    ) -> typing_extensions.Literal["client_type"] | None: ...

global___AnalyzePlanRequest = AnalyzePlanRequest

@typing_extensions.final
class AnalyzePlanResponse(google.protobuf.message.Message):
    """Response to performing analysis of the query. Contains relevant metadata to be able to
    reason about the performance.
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    CLIENT_ID_FIELD_NUMBER: builtins.int
    SCHEMA_FIELD_NUMBER: builtins.int
    EXPLAIN_STRING_FIELD_NUMBER: builtins.int
    TREE_STRING_FIELD_NUMBER: builtins.int
    IS_LOCAL_FIELD_NUMBER: builtins.int
    IS_STREAMING_FIELD_NUMBER: builtins.int
    INPUT_FILES_FIELD_NUMBER: builtins.int
    client_id: builtins.str
    @property
    def schema(self) -> pyspark.sql.connect.proto.types_pb2.DataType: ...
    explain_string: builtins.str
    """The extended explain string as produced by Spark."""
    tree_string: builtins.str
    """Get the tree string of the schema."""
    is_local: builtins.bool
    """Whether the 'collect' and 'take' methods can be run locally."""
    is_streaming: builtins.bool
    """Whether this plan contains one or more sources that continuously
    return data as it arrives.
    """
    @property
    def input_files(
        self,
    ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
        """A best-effort snapshot of the files that compose this Dataset"""
    def __init__(
        self,
        *,
        client_id: builtins.str = ...,
        schema: pyspark.sql.connect.proto.types_pb2.DataType | None = ...,
        explain_string: builtins.str = ...,
        tree_string: builtins.str = ...,
        is_local: builtins.bool = ...,
        is_streaming: builtins.bool = ...,
        input_files: collections.abc.Iterable[builtins.str] | None = ...,
    ) -> None: ...
    def HasField(
        self, field_name: typing_extensions.Literal["schema", b"schema"]
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "client_id",
            b"client_id",
            "explain_string",
            b"explain_string",
            "input_files",
            b"input_files",
            "is_local",
            b"is_local",
            "is_streaming",
            b"is_streaming",
            "schema",
            b"schema",
            "tree_string",
            b"tree_string",
        ],
    ) -> None: ...

global___AnalyzePlanResponse = AnalyzePlanResponse

@typing_extensions.final
class ExecutePlanRequest(google.protobuf.message.Message):
    """A request to be executed by the service."""

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    CLIENT_ID_FIELD_NUMBER: builtins.int
    USER_CONTEXT_FIELD_NUMBER: builtins.int
    PLAN_FIELD_NUMBER: builtins.int
    CLIENT_TYPE_FIELD_NUMBER: builtins.int
    client_id: builtins.str
    """(Required)

    The client_id is set by the client to be able to collate streaming responses from
    different queries.
    """
    @property
    def user_context(self) -> global___UserContext:
        """(Required) User context"""
    @property
    def plan(self) -> global___Plan:
        """(Required) The logical plan to be executed / analyzed."""
    client_type: builtins.str
    """Provides optional information about the client sending the request. This field
    can be used for language or version specific information and is only intended for
    logging purposes and will not be interpreted by the server.
    """
    def __init__(
        self,
        *,
        client_id: builtins.str = ...,
        user_context: global___UserContext | None = ...,
        plan: global___Plan | None = ...,
        client_type: builtins.str | None = ...,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions.Literal[
            "_client_type",
            b"_client_type",
            "client_type",
            b"client_type",
            "plan",
            b"plan",
            "user_context",
            b"user_context",
        ],
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "_client_type",
            b"_client_type",
            "client_id",
            b"client_id",
            "client_type",
            b"client_type",
            "plan",
            b"plan",
            "user_context",
            b"user_context",
        ],
    ) -> None: ...
    def WhichOneof(
        self, oneof_group: typing_extensions.Literal["_client_type", b"_client_type"]
    ) -> typing_extensions.Literal["client_type"] | None: ...

global___ExecutePlanRequest = ExecutePlanRequest

@typing_extensions.final
class ExecutePlanResponse(google.protobuf.message.Message):
    """The response of a query, can be one or more for each request. Responses belonging to the
    same input query, carry the same `client_id`.
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    @typing_extensions.final
    class ArrowBatch(google.protobuf.message.Message):
        """Batch results of metrics."""

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        ROW_COUNT_FIELD_NUMBER: builtins.int
        DATA_FIELD_NUMBER: builtins.int
        row_count: builtins.int
        data: builtins.bytes
        def __init__(
            self,
            *,
            row_count: builtins.int = ...,
            data: builtins.bytes = ...,
        ) -> None: ...
        def ClearField(
            self, field_name: typing_extensions.Literal["data", b"data", "row_count", b"row_count"]
        ) -> None: ...

    @typing_extensions.final
    class Metrics(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        @typing_extensions.final
        class MetricObject(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            @typing_extensions.final
            class ExecutionMetricsEntry(google.protobuf.message.Message):
                DESCRIPTOR: google.protobuf.descriptor.Descriptor

                KEY_FIELD_NUMBER: builtins.int
                VALUE_FIELD_NUMBER: builtins.int
                key: builtins.str
                @property
                def value(self) -> global___ExecutePlanResponse.Metrics.MetricValue: ...
                def __init__(
                    self,
                    *,
                    key: builtins.str = ...,
                    value: global___ExecutePlanResponse.Metrics.MetricValue | None = ...,
                ) -> None: ...
                def HasField(
                    self, field_name: typing_extensions.Literal["value", b"value"]
                ) -> builtins.bool: ...
                def ClearField(
                    self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]
                ) -> None: ...

            NAME_FIELD_NUMBER: builtins.int
            PLAN_ID_FIELD_NUMBER: builtins.int
            PARENT_FIELD_NUMBER: builtins.int
            EXECUTION_METRICS_FIELD_NUMBER: builtins.int
            name: builtins.str
            plan_id: builtins.int
            parent: builtins.int
            @property
            def execution_metrics(
                self,
            ) -> google.protobuf.internal.containers.MessageMap[
                builtins.str, global___ExecutePlanResponse.Metrics.MetricValue
            ]: ...
            def __init__(
                self,
                *,
                name: builtins.str = ...,
                plan_id: builtins.int = ...,
                parent: builtins.int = ...,
                execution_metrics: collections.abc.Mapping[
                    builtins.str, global___ExecutePlanResponse.Metrics.MetricValue
                ]
                | None = ...,
            ) -> None: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "execution_metrics",
                    b"execution_metrics",
                    "name",
                    b"name",
                    "parent",
                    b"parent",
                    "plan_id",
                    b"plan_id",
                ],
            ) -> None: ...

        @typing_extensions.final
        class MetricValue(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            NAME_FIELD_NUMBER: builtins.int
            VALUE_FIELD_NUMBER: builtins.int
            METRIC_TYPE_FIELD_NUMBER: builtins.int
            name: builtins.str
            value: builtins.int
            metric_type: builtins.str
            def __init__(
                self,
                *,
                name: builtins.str = ...,
                value: builtins.int = ...,
                metric_type: builtins.str = ...,
            ) -> None: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "metric_type", b"metric_type", "name", b"name", "value", b"value"
                ],
            ) -> None: ...

        METRICS_FIELD_NUMBER: builtins.int
        @property
        def metrics(
            self,
        ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
            global___ExecutePlanResponse.Metrics.MetricObject
        ]: ...
        def __init__(
            self,
            *,
            metrics: collections.abc.Iterable[global___ExecutePlanResponse.Metrics.MetricObject]
            | None = ...,
        ) -> None: ...
        def ClearField(
            self, field_name: typing_extensions.Literal["metrics", b"metrics"]
        ) -> None: ...

    CLIENT_ID_FIELD_NUMBER: builtins.int
    ARROW_BATCH_FIELD_NUMBER: builtins.int
    METRICS_FIELD_NUMBER: builtins.int
    client_id: builtins.str
    @property
    def arrow_batch(self) -> global___ExecutePlanResponse.ArrowBatch: ...
    @property
    def metrics(self) -> global___ExecutePlanResponse.Metrics:
        """Metrics for the query execution. Typically, this field is only present in the last
        batch of results and then represent the overall state of the query execution.
        """
    def __init__(
        self,
        *,
        client_id: builtins.str = ...,
        arrow_batch: global___ExecutePlanResponse.ArrowBatch | None = ...,
        metrics: global___ExecutePlanResponse.Metrics | None = ...,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions.Literal["arrow_batch", b"arrow_batch", "metrics", b"metrics"],
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "arrow_batch", b"arrow_batch", "client_id", b"client_id", "metrics", b"metrics"
        ],
    ) -> None: ...

global___ExecutePlanResponse = ExecutePlanResponse
