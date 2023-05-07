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
"""
from google.protobuf.descriptor import (
    Descriptor as google___protobuf___descriptor___Descriptor,
    FileDescriptor as google___protobuf___descriptor___FileDescriptor,
)

from google.protobuf.internal.containers import (
    RepeatedCompositeFieldContainer as google___protobuf___internal___containers___RepeatedCompositeFieldContainer,
)

from google.protobuf.message import (
    Message as google___protobuf___message___Message,
)

from typing import (
    Iterable as typing___Iterable,
    Optional as typing___Optional,
    Text as typing___Text,
    overload as typing___overload,
)

from typing_extensions import (
    Literal as typing_extensions___Literal,
)

builtin___bool = bool
builtin___bytes = bytes
builtin___float = float
builtin___int = int

DESCRIPTOR: google___protobuf___descriptor___FileDescriptor = ...

class DataType(google___protobuf___message___Message):
    DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...

    class Boolean(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Boolean = Boolean

    class Byte(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Byte = Byte

    class Short(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Short = Short

    class Integer(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Integer = Integer

    class Long(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Long = Long

    class Float(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Float = Float

    class Double(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Double = Double

    class String(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___String = String

    class Binary(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Binary = Binary

    class NULL(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___NULL = NULL

    class Timestamp(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Timestamp = Timestamp

    class Date(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Date = Date

    class TimestampNTZ(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___TimestampNTZ = TimestampNTZ

    class CalendarInterval(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___CalendarInterval = CalendarInterval

    class YearMonthInterval(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        start_field: builtin___int = ...
        end_field: builtin___int = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            start_field: typing___Optional[builtin___int] = None,
            end_field: typing___Optional[builtin___int] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "_end_field",
                b"_end_field",
                "_start_field",
                b"_start_field",
                "end_field",
                b"end_field",
                "start_field",
                b"start_field",
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "_end_field",
                b"_end_field",
                "_start_field",
                b"_start_field",
                "end_field",
                b"end_field",
                "start_field",
                b"start_field",
                "type_variation_reference",
                b"type_variation_reference",
            ],
        ) -> None: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_end_field", b"_end_field"]
        ) -> typing_extensions___Literal["end_field"]: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_start_field", b"_start_field"]
        ) -> typing_extensions___Literal["start_field"]: ...
    type___YearMonthInterval = YearMonthInterval

    class DayTimeInterval(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        start_field: builtin___int = ...
        end_field: builtin___int = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            start_field: typing___Optional[builtin___int] = None,
            end_field: typing___Optional[builtin___int] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "_end_field",
                b"_end_field",
                "_start_field",
                b"_start_field",
                "end_field",
                b"end_field",
                "start_field",
                b"start_field",
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "_end_field",
                b"_end_field",
                "_start_field",
                b"_start_field",
                "end_field",
                b"end_field",
                "start_field",
                b"start_field",
                "type_variation_reference",
                b"type_variation_reference",
            ],
        ) -> None: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_end_field", b"_end_field"]
        ) -> typing_extensions___Literal["end_field"]: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_start_field", b"_start_field"]
        ) -> typing_extensions___Literal["start_field"]: ...
    type___DayTimeInterval = DayTimeInterval

    class Char(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        length: builtin___int = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            length: typing___Optional[builtin___int] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "length", b"length", "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Char = Char

    class VarChar(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        length: builtin___int = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            length: typing___Optional[builtin___int] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "length", b"length", "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___VarChar = VarChar

    class Decimal(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        scale: builtin___int = ...
        precision: builtin___int = ...
        type_variation_reference: builtin___int = ...

        def __init__(
            self,
            *,
            scale: typing___Optional[builtin___int] = None,
            precision: typing___Optional[builtin___int] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "_precision",
                b"_precision",
                "_scale",
                b"_scale",
                "precision",
                b"precision",
                "scale",
                b"scale",
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "_precision",
                b"_precision",
                "_scale",
                b"_scale",
                "precision",
                b"precision",
                "scale",
                b"scale",
                "type_variation_reference",
                b"type_variation_reference",
            ],
        ) -> None: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_precision", b"_precision"]
        ) -> typing_extensions___Literal["precision"]: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_scale", b"_scale"]
        ) -> typing_extensions___Literal["scale"]: ...
    type___Decimal = Decimal

    class StructField(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        name: typing___Text = ...
        nullable: builtin___bool = ...
        metadata: typing___Text = ...

        @property
        def data_type(self) -> type___DataType: ...
        def __init__(
            self,
            *,
            name: typing___Optional[typing___Text] = None,
            data_type: typing___Optional[type___DataType] = None,
            nullable: typing___Optional[builtin___bool] = None,
            metadata: typing___Optional[typing___Text] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "_metadata", b"_metadata", "data_type", b"data_type", "metadata", b"metadata"
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "_metadata",
                b"_metadata",
                "data_type",
                b"data_type",
                "metadata",
                b"metadata",
                "name",
                b"name",
                "nullable",
                b"nullable",
            ],
        ) -> None: ...
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_metadata", b"_metadata"]
        ) -> typing_extensions___Literal["metadata"]: ...
    type___StructField = StructField

    class Struct(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type_variation_reference: builtin___int = ...

        @property
        def fields(
            self,
        ) -> google___protobuf___internal___containers___RepeatedCompositeFieldContainer[
            type___DataType.StructField
        ]: ...
        def __init__(
            self,
            *,
            fields: typing___Optional[typing___Iterable[type___DataType.StructField]] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "fields", b"fields", "type_variation_reference", b"type_variation_reference"
            ],
        ) -> None: ...
    type___Struct = Struct

    class Array(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        contains_null: builtin___bool = ...
        type_variation_reference: builtin___int = ...

        @property
        def element_type(self) -> type___DataType: ...
        def __init__(
            self,
            *,
            element_type: typing___Optional[type___DataType] = None,
            contains_null: typing___Optional[builtin___bool] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def HasField(
            self, field_name: typing_extensions___Literal["element_type", b"element_type"]
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "contains_null",
                b"contains_null",
                "element_type",
                b"element_type",
                "type_variation_reference",
                b"type_variation_reference",
            ],
        ) -> None: ...
    type___Array = Array

    class Map(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        value_contains_null: builtin___bool = ...
        type_variation_reference: builtin___int = ...

        @property
        def key_type(self) -> type___DataType: ...
        @property
        def value_type(self) -> type___DataType: ...
        def __init__(
            self,
            *,
            key_type: typing___Optional[type___DataType] = None,
            value_type: typing___Optional[type___DataType] = None,
            value_contains_null: typing___Optional[builtin___bool] = None,
            type_variation_reference: typing___Optional[builtin___int] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "key_type", b"key_type", "value_type", b"value_type"
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "key_type",
                b"key_type",
                "type_variation_reference",
                b"type_variation_reference",
                "value_contains_null",
                b"value_contains_null",
                "value_type",
                b"value_type",
            ],
        ) -> None: ...
    type___Map = Map

    class UDT(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        type: typing___Text = ...
        jvm_class: typing___Text = ...
        python_class: typing___Text = ...
        serialized_python_class: typing___Text = ...

        @property
        def sql_type(self) -> type___DataType: ...
        def __init__(
            self,
            *,
            type: typing___Optional[typing___Text] = None,
            jvm_class: typing___Optional[typing___Text] = None,
            python_class: typing___Optional[typing___Text] = None,
            serialized_python_class: typing___Optional[typing___Text] = None,
            sql_type: typing___Optional[type___DataType] = None,
        ) -> None: ...
        def HasField(
            self,
            field_name: typing_extensions___Literal[
                "_jvm_class",
                b"_jvm_class",
                "_python_class",
                b"_python_class",
                "_serialized_python_class",
                b"_serialized_python_class",
                "jvm_class",
                b"jvm_class",
                "python_class",
                b"python_class",
                "serialized_python_class",
                b"serialized_python_class",
                "sql_type",
                b"sql_type",
            ],
        ) -> builtin___bool: ...
        def ClearField(
            self,
            field_name: typing_extensions___Literal[
                "_jvm_class",
                b"_jvm_class",
                "_python_class",
                b"_python_class",
                "_serialized_python_class",
                b"_serialized_python_class",
                "jvm_class",
                b"jvm_class",
                "python_class",
                b"python_class",
                "serialized_python_class",
                b"serialized_python_class",
                "sql_type",
                b"sql_type",
                "type",
                b"type",
            ],
        ) -> None: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_jvm_class", b"_jvm_class"]
        ) -> typing_extensions___Literal["jvm_class"]: ...
        @typing___overload
        def WhichOneof(
            self, oneof_group: typing_extensions___Literal["_python_class", b"_python_class"]
        ) -> typing_extensions___Literal["python_class"]: ...
        @typing___overload
        def WhichOneof(
            self,
            oneof_group: typing_extensions___Literal[
                "_serialized_python_class", b"_serialized_python_class"
            ],
        ) -> typing_extensions___Literal["serialized_python_class"]: ...
    type___UDT = UDT

    class Unparsed(google___protobuf___message___Message):
        DESCRIPTOR: google___protobuf___descriptor___Descriptor = ...
        data_type_string: typing___Text = ...

        def __init__(
            self,
            *,
            data_type_string: typing___Optional[typing___Text] = None,
        ) -> None: ...
        def ClearField(
            self, field_name: typing_extensions___Literal["data_type_string", b"data_type_string"]
        ) -> None: ...
    type___Unparsed = Unparsed

    @property
    def null(self) -> type___DataType.NULL: ...
    @property
    def binary(self) -> type___DataType.Binary: ...
    @property
    def boolean(self) -> type___DataType.Boolean: ...
    @property
    def byte(self) -> type___DataType.Byte: ...
    @property
    def short(self) -> type___DataType.Short: ...
    @property
    def integer(self) -> type___DataType.Integer: ...
    @property
    def long(self) -> type___DataType.Long: ...
    @property
    def float(self) -> type___DataType.Float: ...
    @property
    def double(self) -> type___DataType.Double: ...
    @property
    def decimal(self) -> type___DataType.Decimal: ...
    @property
    def string(self) -> type___DataType.String: ...
    @property
    def char(self) -> type___DataType.Char: ...
    @property
    def var_char(self) -> type___DataType.VarChar: ...
    @property
    def date(self) -> type___DataType.Date: ...
    @property
    def timestamp(self) -> type___DataType.Timestamp: ...
    @property
    def timestamp_ntz(self) -> type___DataType.TimestampNTZ: ...
    @property
    def calendar_interval(self) -> type___DataType.CalendarInterval: ...
    @property
    def year_month_interval(self) -> type___DataType.YearMonthInterval: ...
    @property
    def day_time_interval(self) -> type___DataType.DayTimeInterval: ...
    @property
    def array(self) -> type___DataType.Array: ...
    @property
    def struct(self) -> type___DataType.Struct: ...
    @property
    def map(self) -> type___DataType.Map: ...
    @property
    def udt(self) -> type___DataType.UDT: ...
    @property
    def unparsed(self) -> type___DataType.Unparsed: ...
    def __init__(
        self,
        *,
        null: typing___Optional[type___DataType.NULL] = None,
        binary: typing___Optional[type___DataType.Binary] = None,
        boolean: typing___Optional[type___DataType.Boolean] = None,
        byte: typing___Optional[type___DataType.Byte] = None,
        short: typing___Optional[type___DataType.Short] = None,
        integer: typing___Optional[type___DataType.Integer] = None,
        long: typing___Optional[type___DataType.Long] = None,
        float: typing___Optional[type___DataType.Float] = None,
        double: typing___Optional[type___DataType.Double] = None,
        decimal: typing___Optional[type___DataType.Decimal] = None,
        string: typing___Optional[type___DataType.String] = None,
        char: typing___Optional[type___DataType.Char] = None,
        var_char: typing___Optional[type___DataType.VarChar] = None,
        date: typing___Optional[type___DataType.Date] = None,
        timestamp: typing___Optional[type___DataType.Timestamp] = None,
        timestamp_ntz: typing___Optional[type___DataType.TimestampNTZ] = None,
        calendar_interval: typing___Optional[type___DataType.CalendarInterval] = None,
        year_month_interval: typing___Optional[type___DataType.YearMonthInterval] = None,
        day_time_interval: typing___Optional[type___DataType.DayTimeInterval] = None,
        array: typing___Optional[type___DataType.Array] = None,
        struct: typing___Optional[type___DataType.Struct] = None,
        map: typing___Optional[type___DataType.Map] = None,
        udt: typing___Optional[type___DataType.UDT] = None,
        unparsed: typing___Optional[type___DataType.Unparsed] = None,
    ) -> None: ...
    def HasField(
        self,
        field_name: typing_extensions___Literal[
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
            "char",
            b"char",
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
            "kind",
            b"kind",
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
            "udt",
            b"udt",
            "unparsed",
            b"unparsed",
            "var_char",
            b"var_char",
            "year_month_interval",
            b"year_month_interval",
        ],
    ) -> builtin___bool: ...
    def ClearField(
        self,
        field_name: typing_extensions___Literal[
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
            "char",
            b"char",
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
            "kind",
            b"kind",
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
            "udt",
            b"udt",
            "unparsed",
            b"unparsed",
            "var_char",
            b"var_char",
            "year_month_interval",
            b"year_month_interval",
        ],
    ) -> None: ...
    def WhichOneof(
        self, oneof_group: typing_extensions___Literal["kind", b"kind"]
    ) -> typing_extensions___Literal[
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
        "char",
        "var_char",
        "date",
        "timestamp",
        "timestamp_ntz",
        "calendar_interval",
        "year_month_interval",
        "day_time_interval",
        "array",
        "struct",
        "map",
        "udt",
        "unparsed",
    ]: ...

type___DataType = DataType
