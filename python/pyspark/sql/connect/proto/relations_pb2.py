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
# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: spark/connect/relations.proto
"""Generated protocol buffer code."""
from google.protobuf.internal import builder as _builder
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from pyspark.sql.connect.proto import expressions_pb2 as spark_dot_connect_dot_expressions__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x1dspark/connect/relations.proto\x12\rspark.connect\x1a\x1fspark/connect/expressions.proto"\x8f\x06\n\x08Relation\x12\x35\n\x06\x63ommon\x18\x01 \x01(\x0b\x32\x1d.spark.connect.RelationCommonR\x06\x63ommon\x12)\n\x04read\x18\x02 \x01(\x0b\x32\x13.spark.connect.ReadH\x00R\x04read\x12\x32\n\x07project\x18\x03 \x01(\x0b\x32\x16.spark.connect.ProjectH\x00R\x07project\x12/\n\x06\x66ilter\x18\x04 \x01(\x0b\x32\x15.spark.connect.FilterH\x00R\x06\x66ilter\x12)\n\x04join\x18\x05 \x01(\x0b\x32\x13.spark.connect.JoinH\x00R\x04join\x12,\n\x05union\x18\x06 \x01(\x0b\x32\x14.spark.connect.UnionH\x00R\x05union\x12)\n\x04sort\x18\x07 \x01(\x0b\x32\x13.spark.connect.SortH\x00R\x04sort\x12,\n\x05limit\x18\x08 \x01(\x0b\x32\x14.spark.connect.LimitH\x00R\x05limit\x12\x38\n\taggregate\x18\t \x01(\x0b\x32\x18.spark.connect.AggregateH\x00R\taggregate\x12&\n\x03sql\x18\n \x01(\x0b\x32\x12.spark.connect.SQLH\x00R\x03sql\x12\x45\n\x0elocal_relation\x18\x0b \x01(\x0b\x32\x1c.spark.connect.LocalRelationH\x00R\rlocalRelation\x12/\n\x06sample\x18\x0c \x01(\x0b\x32\x15.spark.connect.SampleH\x00R\x06sample\x12/\n\x06offset\x18\r \x01(\x0b\x32\x15.spark.connect.OffsetH\x00R\x06offset\x12>\n\x0b\x64\x65\x64uplicate\x18\x0e \x01(\x0b\x32\x1a.spark.connect.DeduplicateH\x00R\x0b\x64\x65\x64uplicate\x12\x33\n\x07unknown\x18\xe7\x07 \x01(\x0b\x32\x16.spark.connect.UnknownH\x00R\x07unknownB\n\n\x08rel_type"\t\n\x07Unknown"G\n\x0eRelationCommon\x12\x1f\n\x0bsource_info\x18\x01 \x01(\tR\nsourceInfo\x12\x14\n\x05\x61lias\x18\x02 \x01(\tR\x05\x61lias"\x1b\n\x03SQL\x12\x14\n\x05query\x18\x01 \x01(\tR\x05query"\x9a\x03\n\x04Read\x12\x41\n\x0bnamed_table\x18\x01 \x01(\x0b\x32\x1e.spark.connect.Read.NamedTableH\x00R\nnamedTable\x12\x41\n\x0b\x64\x61ta_source\x18\x02 \x01(\x0b\x32\x1e.spark.connect.Read.DataSourceH\x00R\ndataSource\x1a=\n\nNamedTable\x12/\n\x13unparsed_identifier\x18\x01 \x01(\tR\x12unparsedIdentifier\x1a\xbf\x01\n\nDataSource\x12\x16\n\x06\x66ormat\x18\x01 \x01(\tR\x06\x66ormat\x12\x16\n\x06schema\x18\x02 \x01(\tR\x06schema\x12\x45\n\x07options\x18\x03 \x03(\x0b\x32+.spark.connect.Read.DataSource.OptionsEntryR\x07options\x1a:\n\x0cOptionsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x14\n\x05value\x18\x02 \x01(\tR\x05value:\x02\x38\x01\x42\x0b\n\tread_type"u\n\x07Project\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12;\n\x0b\x65xpressions\x18\x03 \x03(\x0b\x32\x19.spark.connect.ExpressionR\x0b\x65xpressions"p\n\x06\x46ilter\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12\x37\n\tcondition\x18\x02 \x01(\x0b\x32\x19.spark.connect.ExpressionR\tcondition"\x9d\x03\n\x04Join\x12+\n\x04left\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x04left\x12-\n\x05right\x18\x02 \x01(\x0b\x32\x17.spark.connect.RelationR\x05right\x12@\n\x0ejoin_condition\x18\x03 \x01(\x0b\x32\x19.spark.connect.ExpressionR\rjoinCondition\x12\x39\n\tjoin_type\x18\x04 \x01(\x0e\x32\x1c.spark.connect.Join.JoinTypeR\x08joinType"\xbb\x01\n\x08JoinType\x12\x19\n\x15JOIN_TYPE_UNSPECIFIED\x10\x00\x12\x13\n\x0fJOIN_TYPE_INNER\x10\x01\x12\x18\n\x14JOIN_TYPE_FULL_OUTER\x10\x02\x12\x18\n\x14JOIN_TYPE_LEFT_OUTER\x10\x03\x12\x19\n\x15JOIN_TYPE_RIGHT_OUTER\x10\x04\x12\x17\n\x13JOIN_TYPE_LEFT_ANTI\x10\x05\x12\x17\n\x13JOIN_TYPE_LEFT_SEMI\x10\x06"\xcd\x01\n\x05Union\x12/\n\x06inputs\x18\x01 \x03(\x0b\x32\x17.spark.connect.RelationR\x06inputs\x12=\n\nunion_type\x18\x02 \x01(\x0e\x32\x1e.spark.connect.Union.UnionTypeR\tunionType"T\n\tUnionType\x12\x1a\n\x16UNION_TYPE_UNSPECIFIED\x10\x00\x12\x17\n\x13UNION_TYPE_DISTINCT\x10\x01\x12\x12\n\x0eUNION_TYPE_ALL\x10\x02"L\n\x05Limit\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12\x14\n\x05limit\x18\x02 \x01(\x05R\x05limit"O\n\x06Offset\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12\x16\n\x06offset\x18\x02 \x01(\x05R\x06offset"\xc5\x02\n\tAggregate\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12L\n\x14grouping_expressions\x18\x02 \x03(\x0b\x32\x19.spark.connect.ExpressionR\x13groupingExpressions\x12Y\n\x12result_expressions\x18\x03 \x03(\x0b\x32*.spark.connect.Aggregate.AggregateFunctionR\x11resultExpressions\x1a`\n\x11\x41ggregateFunction\x12\x12\n\x04name\x18\x01 \x01(\tR\x04name\x12\x37\n\targuments\x18\x02 \x03(\x0b\x32\x19.spark.connect.ExpressionR\targuments"\xf6\x03\n\x04Sort\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12>\n\x0bsort_fields\x18\x02 \x03(\x0b\x32\x1d.spark.connect.Sort.SortFieldR\nsortFields\x1a\xbc\x01\n\tSortField\x12\x39\n\nexpression\x18\x01 \x01(\x0b\x32\x19.spark.connect.ExpressionR\nexpression\x12?\n\tdirection\x18\x02 \x01(\x0e\x32!.spark.connect.Sort.SortDirectionR\tdirection\x12\x33\n\x05nulls\x18\x03 \x01(\x0e\x32\x1d.spark.connect.Sort.SortNullsR\x05nulls"l\n\rSortDirection\x12\x1e\n\x1aSORT_DIRECTION_UNSPECIFIED\x10\x00\x12\x1c\n\x18SORT_DIRECTION_ASCENDING\x10\x01\x12\x1d\n\x19SORT_DIRECTION_DESCENDING\x10\x02"R\n\tSortNulls\x12\x1a\n\x16SORT_NULLS_UNSPECIFIED\x10\x00\x12\x14\n\x10SORT_NULLS_FIRST\x10\x01\x12\x13\n\x0fSORT_NULLS_LAST\x10\x02"\x8e\x01\n\x0b\x44\x65\x64uplicate\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12!\n\x0c\x63olumn_names\x18\x02 \x03(\tR\x0b\x63olumnNames\x12-\n\x13\x61ll_columns_as_keys\x18\x03 \x01(\x08R\x10\x61llColumnsAsKeys"]\n\rLocalRelation\x12L\n\nattributes\x18\x01 \x03(\x0b\x32,.spark.connect.Expression.QualifiedAttributeR\nattributes"\xf0\x01\n\x06Sample\x12-\n\x05input\x18\x01 \x01(\x0b\x32\x17.spark.connect.RelationR\x05input\x12\x1f\n\x0blower_bound\x18\x02 \x01(\x01R\nlowerBound\x12\x1f\n\x0bupper_bound\x18\x03 \x01(\x01R\nupperBound\x12)\n\x10with_replacement\x18\x04 \x01(\x08R\x0fwithReplacement\x12.\n\x04seed\x18\x05 \x01(\x0b\x32\x1a.spark.connect.Sample.SeedR\x04seed\x1a\x1a\n\x04Seed\x12\x12\n\x04seed\x18\x01 \x01(\x03R\x04seedB"\n\x1eorg.apache.spark.connect.protoP\x01\x62\x06proto3'
)

_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, globals())
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "spark.connect.relations_pb2", globals())
if _descriptor._USE_C_DESCRIPTORS == False:

    DESCRIPTOR._options = None
    DESCRIPTOR._serialized_options = b"\n\036org.apache.spark.connect.protoP\001"
    _READ_DATASOURCE_OPTIONSENTRY._options = None
    _READ_DATASOURCE_OPTIONSENTRY._serialized_options = b"8\001"
    _RELATION._serialized_start = 82
    _RELATION._serialized_end = 865
    _UNKNOWN._serialized_start = 867
    _UNKNOWN._serialized_end = 876
    _RELATIONCOMMON._serialized_start = 878
    _RELATIONCOMMON._serialized_end = 949
    _SQL._serialized_start = 951
    _SQL._serialized_end = 978
    _READ._serialized_start = 981
    _READ._serialized_end = 1391
    _READ_NAMEDTABLE._serialized_start = 1123
    _READ_NAMEDTABLE._serialized_end = 1184
    _READ_DATASOURCE._serialized_start = 1187
    _READ_DATASOURCE._serialized_end = 1378
    _READ_DATASOURCE_OPTIONSENTRY._serialized_start = 1320
    _READ_DATASOURCE_OPTIONSENTRY._serialized_end = 1378
    _PROJECT._serialized_start = 1393
    _PROJECT._serialized_end = 1510
    _FILTER._serialized_start = 1512
    _FILTER._serialized_end = 1624
    _JOIN._serialized_start = 1627
    _JOIN._serialized_end = 2040
    _JOIN_JOINTYPE._serialized_start = 1853
    _JOIN_JOINTYPE._serialized_end = 2040
    _UNION._serialized_start = 2043
    _UNION._serialized_end = 2248
    _UNION_UNIONTYPE._serialized_start = 2164
    _UNION_UNIONTYPE._serialized_end = 2248
    _LIMIT._serialized_start = 2250
    _LIMIT._serialized_end = 2326
    _OFFSET._serialized_start = 2328
    _OFFSET._serialized_end = 2407
    _AGGREGATE._serialized_start = 2410
    _AGGREGATE._serialized_end = 2735
    _AGGREGATE_AGGREGATEFUNCTION._serialized_start = 2639
    _AGGREGATE_AGGREGATEFUNCTION._serialized_end = 2735
    _SORT._serialized_start = 2738
    _SORT._serialized_end = 3240
    _SORT_SORTFIELD._serialized_start = 2858
    _SORT_SORTFIELD._serialized_end = 3046
    _SORT_SORTDIRECTION._serialized_start = 3048
    _SORT_SORTDIRECTION._serialized_end = 3156
    _SORT_SORTNULLS._serialized_start = 3158
    _SORT_SORTNULLS._serialized_end = 3240
    _DEDUPLICATE._serialized_start = 3243
    _DEDUPLICATE._serialized_end = 3385
    _LOCALRELATION._serialized_start = 3387
    _LOCALRELATION._serialized_end = 3480
    _SAMPLE._serialized_start = 3483
    _SAMPLE._serialized_end = 3723
    _SAMPLE_SEED._serialized_start = 3697
    _SAMPLE_SEED._serialized_end = 3723
# @@protoc_insertion_point(module_scope)
