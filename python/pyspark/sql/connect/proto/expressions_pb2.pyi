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
import google.protobuf.message
import pyspark.sql.connect.proto.types_pb2
import sys

if sys.version_info >= (3, 8):
    import typing as typing_extensions
else:
    import typing_extensions

DESCRIPTOR: google.protobuf.descriptor.FileDescriptor

class Expression(google.protobuf.message.Message):
    """Expression used to refer to fields, functions and similar. This can be used everywhere
    expressions in SQL appear.
    """

    DESCRIPTOR: google.protobuf.descriptor.Descriptor

    class Literal(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        class VarChar(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            VALUE_FIELD_NUMBER: builtins.int
            LENGTH_FIELD_NUMBER: builtins.int
            value: builtins.str
            length: builtins.int
            def __init__(
                self,
                *,
                value: builtins.str = ...,
                length: builtins.int = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["length", b"length", "value", b"value"]
            ) -> None: ...

        class Decimal(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            VALUE_FIELD_NUMBER: builtins.int
            PRECISION_FIELD_NUMBER: builtins.int
            SCALE_FIELD_NUMBER: builtins.int
            value: builtins.bytes
            """little-endian twos-complement integer representation of complete value
            (ignoring precision) Always 16 bytes in length
            """
            precision: builtins.int
            """The maximum number of digits allowed in the value.
            the maximum precision is 38.
            """
            scale: builtins.int
            """declared scale of decimal literal"""
            def __init__(
                self,
                *,
                value: builtins.bytes = ...,
                precision: builtins.int = ...,
                scale: builtins.int = ...,
            ) -> None: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "precision", b"precision", "scale", b"scale", "value", b"value"
                ],
            ) -> None: ...

        class Map(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            class KeyValue(google.protobuf.message.Message):
                DESCRIPTOR: google.protobuf.descriptor.Descriptor

                KEY_FIELD_NUMBER: builtins.int
                VALUE_FIELD_NUMBER: builtins.int
                @property
                def key(self) -> global___Expression.Literal: ...
                @property
                def value(self) -> global___Expression.Literal: ...
                def __init__(
                    self,
                    *,
                    key: global___Expression.Literal | None = ...,
                    value: global___Expression.Literal | None = ...,
                ) -> None: ...
                def HasField(
                    self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]
                ) -> builtins.bool: ...
                def ClearField(
                    self, field_name: typing_extensions.Literal["key", b"key", "value", b"value"]
                ) -> None: ...

            KEY_VALUES_FIELD_NUMBER: builtins.int
            @property
            def key_values(
                self,
            ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
                global___Expression.Literal.Map.KeyValue
            ]: ...
            def __init__(
                self,
                *,
                key_values: collections.abc.Iterable[global___Expression.Literal.Map.KeyValue]
                | None = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["key_values", b"key_values"]
            ) -> None: ...

        class IntervalYearToMonth(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            YEARS_FIELD_NUMBER: builtins.int
            MONTHS_FIELD_NUMBER: builtins.int
            years: builtins.int
            months: builtins.int
            def __init__(
                self,
                *,
                years: builtins.int = ...,
                months: builtins.int = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["months", b"months", "years", b"years"]
            ) -> None: ...

        class IntervalDayToSecond(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            DAYS_FIELD_NUMBER: builtins.int
            SECONDS_FIELD_NUMBER: builtins.int
            MICROSECONDS_FIELD_NUMBER: builtins.int
            days: builtins.int
            seconds: builtins.int
            microseconds: builtins.int
            def __init__(
                self,
                *,
                days: builtins.int = ...,
                seconds: builtins.int = ...,
                microseconds: builtins.int = ...,
            ) -> None: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "days", b"days", "microseconds", b"microseconds", "seconds", b"seconds"
                ],
            ) -> None: ...

        class Struct(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            FIELDS_FIELD_NUMBER: builtins.int
            @property
            def fields(
                self,
            ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
                global___Expression.Literal
            ]:
                """A possibly heterogeneously typed list of literals"""
            def __init__(
                self,
                *,
                fields: collections.abc.Iterable[global___Expression.Literal] | None = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["fields", b"fields"]
            ) -> None: ...

        class List(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            VALUES_FIELD_NUMBER: builtins.int
            @property
            def values(
                self,
            ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
                global___Expression.Literal
            ]:
                """A homogeneously typed list of literals"""
            def __init__(
                self,
                *,
                values: collections.abc.Iterable[global___Expression.Literal] | None = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["values", b"values"]
            ) -> None: ...

        class UserDefined(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            TYPE_REFERENCE_FIELD_NUMBER: builtins.int
            VALUE_FIELD_NUMBER: builtins.int
            type_reference: builtins.int
            """points to a type_anchor defined in this plan"""
            @property
            def value(self) -> google.protobuf.any_pb2.Any:
                """the value of the literal, serialized using some type-specific
                protobuf message
                """
            def __init__(
                self,
                *,
                type_reference: builtins.int = ...,
                value: google.protobuf.any_pb2.Any | None = ...,
            ) -> None: ...
            def HasField(
                self, field_name: typing_extensions.Literal["value", b"value"]
            ) -> builtins.bool: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "type_reference", b"type_reference", "value", b"value"
                ],
            ) -> None: ...

        BOOLEAN_FIELD_NUMBER: builtins.int
        I8_FIELD_NUMBER: builtins.int
        I16_FIELD_NUMBER: builtins.int
        I32_FIELD_NUMBER: builtins.int
        I64_FIELD_NUMBER: builtins.int
        FP32_FIELD_NUMBER: builtins.int
        FP64_FIELD_NUMBER: builtins.int
        STRING_FIELD_NUMBER: builtins.int
        BINARY_FIELD_NUMBER: builtins.int
        TIMESTAMP_FIELD_NUMBER: builtins.int
        DATE_FIELD_NUMBER: builtins.int
        TIME_FIELD_NUMBER: builtins.int
        INTERVAL_YEAR_TO_MONTH_FIELD_NUMBER: builtins.int
        INTERVAL_DAY_TO_SECOND_FIELD_NUMBER: builtins.int
        FIXED_CHAR_FIELD_NUMBER: builtins.int
        VAR_CHAR_FIELD_NUMBER: builtins.int
        FIXED_BINARY_FIELD_NUMBER: builtins.int
        DECIMAL_FIELD_NUMBER: builtins.int
        STRUCT_FIELD_NUMBER: builtins.int
        MAP_FIELD_NUMBER: builtins.int
        TIMESTAMP_TZ_FIELD_NUMBER: builtins.int
        UUID_FIELD_NUMBER: builtins.int
        NULL_FIELD_NUMBER: builtins.int
        LIST_FIELD_NUMBER: builtins.int
        EMPTY_LIST_FIELD_NUMBER: builtins.int
        EMPTY_MAP_FIELD_NUMBER: builtins.int
        USER_DEFINED_FIELD_NUMBER: builtins.int
        NULLABLE_FIELD_NUMBER: builtins.int
        TYPE_VARIATION_REFERENCE_FIELD_NUMBER: builtins.int
        boolean: builtins.bool
        i8: builtins.int
        i16: builtins.int
        i32: builtins.int
        i64: builtins.int
        fp32: builtins.float
        fp64: builtins.float
        string: builtins.str
        binary: builtins.bytes
        timestamp: builtins.int
        """Timestamp in units of microseconds since the UNIX epoch."""
        date: builtins.int
        """Date in units of days since the UNIX epoch."""
        time: builtins.int
        """Time in units of microseconds past midnight"""
        @property
        def interval_year_to_month(self) -> global___Expression.Literal.IntervalYearToMonth: ...
        @property
        def interval_day_to_second(self) -> global___Expression.Literal.IntervalDayToSecond: ...
        fixed_char: builtins.str
        @property
        def var_char(self) -> global___Expression.Literal.VarChar: ...
        fixed_binary: builtins.bytes
        @property
        def decimal(self) -> global___Expression.Literal.Decimal: ...
        @property
        def struct(self) -> global___Expression.Literal.Struct: ...
        @property
        def map(self) -> global___Expression.Literal.Map: ...
        timestamp_tz: builtins.int
        """Timestamp in units of microseconds since the UNIX epoch."""
        uuid: builtins.bytes
        @property
        def null(self) -> pyspark.sql.connect.proto.types_pb2.DataType:
            """a typed null literal"""
        @property
        def list(self) -> global___Expression.Literal.List: ...
        @property
        def empty_list(self) -> pyspark.sql.connect.proto.types_pb2.DataType.List: ...
        @property
        def empty_map(self) -> pyspark.sql.connect.proto.types_pb2.DataType.Map: ...
        @property
        def user_defined(self) -> global___Expression.Literal.UserDefined: ...
        nullable: builtins.bool
        """whether the literal type should be treated as a nullable type. Applies to
        all members of union other than the Typed null (which should directly
        declare nullability).
        """
        type_variation_reference: builtins.int
        """optionally points to a type_variation_anchor defined in this plan.
        Applies to all members of union other than the Typed null (which should
        directly declare the type variation).
        """
        def __init__(
            self,
            *,
            boolean: builtins.bool = ...,
            i8: builtins.int = ...,
            i16: builtins.int = ...,
            i32: builtins.int = ...,
            i64: builtins.int = ...,
            fp32: builtins.float = ...,
            fp64: builtins.float = ...,
            string: builtins.str = ...,
            binary: builtins.bytes = ...,
            timestamp: builtins.int = ...,
            date: builtins.int = ...,
            time: builtins.int = ...,
            interval_year_to_month: global___Expression.Literal.IntervalYearToMonth | None = ...,
            interval_day_to_second: global___Expression.Literal.IntervalDayToSecond | None = ...,
            fixed_char: builtins.str = ...,
            var_char: global___Expression.Literal.VarChar | None = ...,
            fixed_binary: builtins.bytes = ...,
            decimal: global___Expression.Literal.Decimal | None = ...,
            struct: global___Expression.Literal.Struct | None = ...,
            map: global___Expression.Literal.Map | None = ...,
            timestamp_tz: builtins.int = ...,
            uuid: builtins.bytes = ...,
            null: pyspark.sql.connect.proto.types_pb2.DataType | None = ...,
            list: global___Expression.Literal.List | None = ...,
            empty_list: pyspark.sql.connect.proto.types_pb2.DataType.List | None = ...,
            empty_map: pyspark.sql.connect.proto.types_pb2.DataType.Map | None = ...,
            user_defined: global___Expression.Literal.UserDefined | None = ...,
            nullable: builtins.bool = ...,
            type_variation_reference: builtins.int = ...,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions.Literal[
                "binary",
                b"binary",
                "boolean",
                b"boolean",
                "date",
                b"date",
                "decimal",
                b"decimal",
                "empty_list",
                b"empty_list",
                "empty_map",
                b"empty_map",
                "fixed_binary",
                b"fixed_binary",
                "fixed_char",
                b"fixed_char",
                "fp32",
                b"fp32",
                "fp64",
                b"fp64",
                "i16",
                b"i16",
                "i32",
                b"i32",
                "i64",
                b"i64",
                "i8",
                b"i8",
                "interval_day_to_second",
                b"interval_day_to_second",
                "interval_year_to_month",
                b"interval_year_to_month",
                "list",
                b"list",
                "literal_type",
                b"literal_type",
                "map",
                b"map",
                "null",
                b"null",
                "string",
                b"string",
                "struct",
                b"struct",
                "time",
                b"time",
                "timestamp",
                b"timestamp",
                "timestamp_tz",
                b"timestamp_tz",
                "user_defined",
                b"user_defined",
                "uuid",
                b"uuid",
                "var_char",
                b"var_char",
            ],
        ) -> builtins.bool: ...
        def ClearField(
            self,
            field_name: typing_extensions.Literal[
                "binary",
                b"binary",
                "boolean",
                b"boolean",
                "date",
                b"date",
                "decimal",
                b"decimal",
                "empty_list",
                b"empty_list",
                "empty_map",
                b"empty_map",
                "fixed_binary",
                b"fixed_binary",
                "fixed_char",
                b"fixed_char",
                "fp32",
                b"fp32",
                "fp64",
                b"fp64",
                "i16",
                b"i16",
                "i32",
                b"i32",
                "i64",
                b"i64",
                "i8",
                b"i8",
                "interval_day_to_second",
                b"interval_day_to_second",
                "interval_year_to_month",
                b"interval_year_to_month",
                "list",
                b"list",
                "literal_type",
                b"literal_type",
                "map",
                b"map",
                "null",
                b"null",
                "nullable",
                b"nullable",
                "string",
                b"string",
                "struct",
                b"struct",
                "time",
                b"time",
                "timestamp",
                b"timestamp",
                "timestamp_tz",
                b"timestamp_tz",
                "type_variation_reference",
                b"type_variation_reference",
                "user_defined",
                b"user_defined",
                "uuid",
                b"uuid",
                "var_char",
                b"var_char",
            ],
        ) -> None: ...
        def WhichOneof(
            self, oneof_group: typing_extensions.Literal["literal_type", b"literal_type"]
        ) -> typing_extensions.Literal[
            "boolean",
            "i8",
            "i16",
            "i32",
            "i64",
            "fp32",
            "fp64",
            "string",
            "binary",
            "timestamp",
            "date",
            "time",
            "interval_year_to_month",
            "interval_day_to_second",
            "fixed_char",
            "var_char",
            "fixed_binary",
            "decimal",
            "struct",
            "map",
            "timestamp_tz",
            "uuid",
            "null",
            "list",
            "empty_list",
            "empty_map",
            "user_defined",
        ] | None: ...

    class UnresolvedAttribute(google.protobuf.message.Message):
        """An unresolved attribute that is not explicitly bound to a specific column, but the column
        is resolved during analysis by name.
        """

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        UNPARSED_IDENTIFIER_FIELD_NUMBER: builtins.int
        unparsed_identifier: builtins.str
        def __init__(
            self,
            *,
            unparsed_identifier: builtins.str = ...,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions.Literal["unparsed_identifier", b"unparsed_identifier"],
        ) -> None: ...

    class UnresolvedFunction(google.protobuf.message.Message):
        """An unresolved function is not explicitly bound to one explicit function, but the function
        is resolved during analysis following Sparks name resolution rules.
        """

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        PARTS_FIELD_NUMBER: builtins.int
        ARGUMENTS_FIELD_NUMBER: builtins.int
        @property
        def parts(
            self,
        ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]: ...
        @property
        def arguments(
            self,
        ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
            global___Expression
        ]: ...
        def __init__(
            self,
            *,
            parts: collections.abc.Iterable[builtins.str] | None = ...,
            arguments: collections.abc.Iterable[global___Expression] | None = ...,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions.Literal["arguments", b"arguments", "parts", b"parts"],
        ) -> None: ...

    class ExpressionString(google.protobuf.message.Message):
        """Expression as string."""

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        EXPRESSION_FIELD_NUMBER: builtins.int
        expression: builtins.str
        def __init__(
            self,
            *,
            expression: builtins.str = ...,
        ) -> None: ...
        def ClearField(
            self, field_name: typing_extensions.Literal["expression", b"expression"]
        ) -> None: ...

    class UnresolvedStar(google.protobuf.message.Message):
        """UnresolvedStar is used to expand all the fields of a relation or struct."""

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        def __init__(
            self,
        ) -> None: ...

    class QualifiedAttribute(google.protobuf.message.Message):
        """An qualified attribute that can specify a reference (e.g. column) without needing a resolution
        by the analyzer.
        """

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        NAME_FIELD_NUMBER: builtins.int
        TYPE_FIELD_NUMBER: builtins.int
        name: builtins.str
        @property
        def type(self) -> pyspark.sql.connect.proto.types_pb2.DataType: ...
        def __init__(
            self,
            *,
            name: builtins.str = ...,
            type: pyspark.sql.connect.proto.types_pb2.DataType | None = ...,
        ) -> None: ...
        def HasField(
            self, field_name: typing_extensions.Literal["type", b"type"]
        ) -> builtins.bool: ...
        def ClearField(
            self, field_name: typing_extensions.Literal["name", b"name", "type", b"type"]
        ) -> None: ...

    class Alias(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        EXPR_FIELD_NUMBER: builtins.int
        NAME_FIELD_NUMBER: builtins.int
        METADATA_FIELD_NUMBER: builtins.int
        @property
        def expr(self) -> global___Expression: ...
        @property
        def name(
            self,
        ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]: ...
        metadata: builtins.str
        """Alias metadata expressed as a JSON map."""
        def __init__(
            self,
            *,
            expr: global___Expression | None = ...,
            name: collections.abc.Iterable[builtins.str] | None = ...,
            metadata: builtins.str | None = ...,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions.Literal[
                "_metadata", b"_metadata", "expr", b"expr", "metadata", b"metadata"
            ],
        ) -> builtins.bool: ...
        def ClearField(
            self,
            field_name: typing_extensions.Literal[
                "_metadata", b"_metadata", "expr", b"expr", "metadata", b"metadata", "name", b"name"
            ],
        ) -> None: ...
        def WhichOneof(
            self, oneof_group: typing_extensions.Literal["_metadata", b"_metadata"]
        ) -> typing_extensions.Literal["metadata"] | None: ...

    LITERAL_FIELD_NUMBER: builtins.int
    UNRESOLVED_ATTRIBUTE_FIELD_NUMBER: builtins.int
    UNRESOLVED_FUNCTION_FIELD_NUMBER: builtins.int
    EXPRESSION_STRING_FIELD_NUMBER: builtins.int
    UNRESOLVED_STAR_FIELD_NUMBER: builtins.int
    ALIAS_FIELD_NUMBER: builtins.int
    @property
    def literal(self) -> global___Expression.Literal: ...
    @property
    def unresolved_attribute(self) -> global___Expression.UnresolvedAttribute: ...
    @property
    def unresolved_function(self) -> global___Expression.UnresolvedFunction: ...
    @property
    def expression_string(self) -> global___Expression.ExpressionString: ...
    @property
    def unresolved_star(self) -> global___Expression.UnresolvedStar: ...
    @property
    def alias(self) -> global___Expression.Alias: ...
    def __init__(
        self,
        *,
        literal: global___Expression.Literal | None = ...,
        unresolved_attribute: global___Expression.UnresolvedAttribute | None = ...,
        unresolved_function: global___Expression.UnresolvedFunction | None = ...,
        expression_string: global___Expression.ExpressionString | None = ...,
        unresolved_star: global___Expression.UnresolvedStar | None = ...,
        alias: global___Expression.Alias | None = ...,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions.Literal[
            "alias",
            b"alias",
            "expr_type",
            b"expr_type",
            "expression_string",
            b"expression_string",
            "literal",
            b"literal",
            "unresolved_attribute",
            b"unresolved_attribute",
            "unresolved_function",
            b"unresolved_function",
            "unresolved_star",
            b"unresolved_star",
        ],
    ) -> builtins.bool: ...
    def ClearField(
        self,
        field_name: typing_extensions.Literal[
            "alias",
            b"alias",
            "expr_type",
            b"expr_type",
            "expression_string",
            b"expression_string",
            "literal",
            b"literal",
            "unresolved_attribute",
            b"unresolved_attribute",
            "unresolved_function",
            b"unresolved_function",
            "unresolved_star",
            b"unresolved_star",
        ],
    ) -> None: ...
    def WhichOneof(
        self, oneof_group: typing_extensions.Literal["expr_type", b"expr_type"]
    ) -> typing_extensions.Literal[
        "literal",
        "unresolved_attribute",
        "unresolved_function",
        "expression_string",
        "unresolved_star",
        "alias",
    ] | None: ...

global___Expression = Expression
