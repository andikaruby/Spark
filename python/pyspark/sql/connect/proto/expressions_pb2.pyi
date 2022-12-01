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
import google.protobuf.descriptor
import google.protobuf.internal.containers
import google.protobuf.message
import sys
import typing

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

        class Decimal(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            VALUE_FIELD_NUMBER: builtins.int
            PRECISION_FIELD_NUMBER: builtins.int
            SCALE_FIELD_NUMBER: builtins.int
            value: builtins.str
            """the string representation."""
            precision: builtins.int
            """The maximum number of digits allowed in the value.
            the maximum precision is 38.
            """
            scale: builtins.int
            """declared scale of decimal literal"""
            def __init__(
                self,
                *,
                value: builtins.str = ...,
                precision: builtins.int | None = ...,
                scale: builtins.int | None = ...,
            ) -> None: ...
            def HasField(
                self,
                field_name: typing_extensions.Literal[
                    "_precision",
                    b"_precision",
                    "_scale",
                    b"_scale",
                    "precision",
                    b"precision",
                    "scale",
                    b"scale",
                ],
            ) -> builtins.bool: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "_precision",
                    b"_precision",
                    "_scale",
                    b"_scale",
                    "precision",
                    b"precision",
                    "scale",
                    b"scale",
                    "value",
                    b"value",
                ],
            ) -> None: ...
            @typing.overload
            def WhichOneof(
                self, oneof_group: typing_extensions.Literal["_precision", b"_precision"]
            ) -> typing_extensions.Literal["precision"] | None: ...
            @typing.overload
            def WhichOneof(
                self, oneof_group: typing_extensions.Literal["_scale", b"_scale"]
            ) -> typing_extensions.Literal["scale"] | None: ...

        class CalendarInterval(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            MONTHS_FIELD_NUMBER: builtins.int
            DAYS_FIELD_NUMBER: builtins.int
            MICROSECONDS_FIELD_NUMBER: builtins.int
            months: builtins.int
            days: builtins.int
            microseconds: builtins.int
            def __init__(
                self,
                *,
                months: builtins.int = ...,
                days: builtins.int = ...,
                microseconds: builtins.int = ...,
            ) -> None: ...
            def ClearField(
                self,
                field_name: typing_extensions.Literal[
                    "days", b"days", "microseconds", b"microseconds", "months", b"months"
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

        class Array(google.protobuf.message.Message):
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

        class Map(google.protobuf.message.Message):
            DESCRIPTOR: google.protobuf.descriptor.Descriptor

            class Pair(google.protobuf.message.Message):
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

            PAIRS_FIELD_NUMBER: builtins.int
            @property
            def pairs(
                self,
            ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
                global___Expression.Literal.Map.Pair
            ]: ...
            def __init__(
                self,
                *,
                pairs: collections.abc.Iterable[global___Expression.Literal.Map.Pair] | None = ...,
            ) -> None: ...
            def ClearField(
                self, field_name: typing_extensions.Literal["pairs", b"pairs"]
            ) -> None: ...

        NULL_FIELD_NUMBER: builtins.int
        BINARY_FIELD_NUMBER: builtins.int
        BOOLEAN_FIELD_NUMBER: builtins.int
        BYTE_FIELD_NUMBER: builtins.int
        SHORT_FIELD_NUMBER: builtins.int
        INTEGER_FIELD_NUMBER: builtins.int
        LONG_FIELD_NUMBER: builtins.int
        FLOAT_FIELD_NUMBER: builtins.int
        DOUBLE_FIELD_NUMBER: builtins.int
        DECIMAL_FIELD_NUMBER: builtins.int
        STRING_FIELD_NUMBER: builtins.int
        DATE_FIELD_NUMBER: builtins.int
        TIMESTAMP_FIELD_NUMBER: builtins.int
        TIMESTAMP_NTZ_FIELD_NUMBER: builtins.int
        CALENDAR_INTERVAL_FIELD_NUMBER: builtins.int
        YEAR_MONTH_INTERVAL_FIELD_NUMBER: builtins.int
        DAY_TIME_INTERVAL_FIELD_NUMBER: builtins.int
        ARRAY_FIELD_NUMBER: builtins.int
        STRUCT_FIELD_NUMBER: builtins.int
        MAP_FIELD_NUMBER: builtins.int
        NULLABLE_FIELD_NUMBER: builtins.int
        TYPE_VARIATION_REFERENCE_FIELD_NUMBER: builtins.int
        null: builtins.bool
        binary: builtins.bytes
        boolean: builtins.bool
        byte: builtins.int
        short: builtins.int
        integer: builtins.int
        long: builtins.int
        float: builtins.float
        double: builtins.float
        @property
        def decimal(self) -> global___Expression.Literal.Decimal: ...
        string: builtins.str
        date: builtins.int
        """Date in units of days since the UNIX epoch."""
        timestamp: builtins.int
        """Timestamp in units of microseconds since the UNIX epoch."""
        timestamp_ntz: builtins.int
        """Timestamp in units of microseconds since the UNIX epoch (without timezone information)."""
        @property
        def calendar_interval(self) -> global___Expression.Literal.CalendarInterval: ...
        year_month_interval: builtins.int
        day_time_interval: builtins.int
        @property
        def array(self) -> global___Expression.Literal.Array: ...
        @property
        def struct(self) -> global___Expression.Literal.Struct: ...
        @property
        def map(self) -> global___Expression.Literal.Map: ...
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
            null: builtins.bool = ...,
            binary: builtins.bytes = ...,
            boolean: builtins.bool = ...,
            byte: builtins.int = ...,
            short: builtins.int = ...,
            integer: builtins.int = ...,
            long: builtins.int = ...,
            float: builtins.float = ...,
            double: builtins.float = ...,
            decimal: global___Expression.Literal.Decimal | None = ...,
            string: builtins.str = ...,
            date: builtins.int = ...,
            timestamp: builtins.int = ...,
            timestamp_ntz: builtins.int = ...,
            calendar_interval: global___Expression.Literal.CalendarInterval | None = ...,
            year_month_interval: builtins.int = ...,
            day_time_interval: builtins.int = ...,
            array: global___Expression.Literal.Array | None = ...,
            struct: global___Expression.Literal.Struct | None = ...,
            map: global___Expression.Literal.Map | None = ...,
            nullable: builtins.bool = ...,
            type_variation_reference: builtins.int = ...,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions.Literal[
                "array",
                b"array",
                "binary",
                b"binary",
                "boolean",
                b"boolean",
                "byte",
                b"byte",
                "calendar_interval",
                b"calendar_interval",
                "date",
                b"date",
                "day_time_interval",
                b"day_time_interval",
                "decimal",
                b"decimal",
                "double",
                b"double",
                "float",
                b"float",
                "integer",
                b"integer",
                "literal_type",
                b"literal_type",
                "long",
                b"long",
                "map",
                b"map",
                "null",
                b"null",
                "short",
                b"short",
                "string",
                b"string",
                "struct",
                b"struct",
                "timestamp",
                b"timestamp",
                "timestamp_ntz",
                b"timestamp_ntz",
                "year_month_interval",
                b"year_month_interval",
            ],
        ) -> builtins.bool: ...
        def ClearField(
            self,
            field_name: typing_extensions.Literal[
                "array",
                b"array",
                "binary",
                b"binary",
                "boolean",
                b"boolean",
                "byte",
                b"byte",
                "calendar_interval",
                b"calendar_interval",
                "date",
                b"date",
                "day_time_interval",
                b"day_time_interval",
                "decimal",
                b"decimal",
                "double",
                b"double",
                "float",
                b"float",
                "integer",
                b"integer",
                "literal_type",
                b"literal_type",
                "long",
                b"long",
                "map",
                b"map",
                "null",
                b"null",
                "nullable",
                b"nullable",
                "short",
                b"short",
                "string",
                b"string",
                "struct",
                b"struct",
                "timestamp",
                b"timestamp",
                "timestamp_ntz",
                b"timestamp_ntz",
                "type_variation_reference",
                b"type_variation_reference",
                "year_month_interval",
                b"year_month_interval",
            ],
        ) -> None: ...
        def WhichOneof(
            self, oneof_group: typing_extensions.Literal["literal_type", b"literal_type"]
        ) -> typing_extensions.Literal[
            "null",
            "binary",
            "boolean",
            "byte",
            "short",
            "integer",
            "long",
            "float",
            "double",
            "decimal",
            "string",
            "date",
            "timestamp",
            "timestamp_ntz",
            "calendar_interval",
            "year_month_interval",
            "day_time_interval",
            "array",
            "struct",
            "map",
        ] | None: ...

    class UnresolvedAttribute(google.protobuf.message.Message):
        """An unresolved attribute that is not explicitly bound to a specific column, but the column
        is resolved during analysis by name.
        """

        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        UNPARSED_IDENTIFIER_FIELD_NUMBER: builtins.int
        unparsed_identifier: builtins.str
        """(Required) An identifier that will be parsed by Catalyst parser. This should follow the
        Spark SQL identifier syntax.
        """
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
        ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
            """(Required) Names parts for the unresolved function."""
        @property
        def arguments(
            self,
        ) -> google.protobuf.internal.containers.RepeatedCompositeFieldContainer[
            global___Expression
        ]:
            """(Optional) Function arguments. Empty arguments are allowed."""
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
        """(Required) A SQL expression that will be parsed by Catalyst parser."""
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

        TARGET_FIELD_NUMBER: builtins.int
        @property
        def target(
            self,
        ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
            """(Optional) The target of the expansion, either be a table name or struct name, this
            is a list of identifiers that is the path of the expansion.
            """
        def __init__(
            self,
            *,
            target: collections.abc.Iterable[builtins.str] | None = ...,
        ) -> None: ...
        def ClearField(
            self, field_name: typing_extensions.Literal["target", b"target"]
        ) -> None: ...

    class Alias(google.protobuf.message.Message):
        DESCRIPTOR: google.protobuf.descriptor.Descriptor

        EXPR_FIELD_NUMBER: builtins.int
        NAME_FIELD_NUMBER: builtins.int
        METADATA_FIELD_NUMBER: builtins.int
        @property
        def expr(self) -> global___Expression:
            """(Required) The expression that alias will be added on."""
        @property
        def name(
            self,
        ) -> google.protobuf.internal.containers.RepeatedScalarFieldContainer[builtins.str]:
            """(Required) a list of name parts for the alias.

            Scalar columns only has one name that presents.
            """
        metadata: builtins.str
        """(Optional) Alias metadata expressed as a JSON map."""
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
