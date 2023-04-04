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
# source: spark/connect/catalog.proto
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from pyspark.sql.connect.proto import common_pb2 as spark_dot_connect_dot_common__pb2
from pyspark.sql.connect.proto import types_pb2 as spark_dot_connect_dot_types__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b'\n\x1bspark/connect/catalog.proto\x12\rspark.connect\x1a\x1aspark/connect/common.proto\x1a\x19spark/connect/types.proto"\xc6\x0e\n\x07\x43\x61talog\x12K\n\x10\x63urrent_database\x18\x01 \x01(\x0b\x32\x1e.spark.connect.CurrentDatabaseH\x00R\x0f\x63urrentDatabase\x12U\n\x14set_current_database\x18\x02 \x01(\x0b\x32!.spark.connect.SetCurrentDatabaseH\x00R\x12setCurrentDatabase\x12\x45\n\x0elist_databases\x18\x03 \x01(\x0b\x32\x1c.spark.connect.ListDatabasesH\x00R\rlistDatabases\x12<\n\x0blist_tables\x18\x04 \x01(\x0b\x32\x19.spark.connect.ListTablesH\x00R\nlistTables\x12\x45\n\x0elist_functions\x18\x05 \x01(\x0b\x32\x1c.spark.connect.ListFunctionsH\x00R\rlistFunctions\x12?\n\x0clist_columns\x18\x06 \x01(\x0b\x32\x1a.spark.connect.ListColumnsH\x00R\x0blistColumns\x12?\n\x0cget_database\x18\x07 \x01(\x0b\x32\x1a.spark.connect.GetDatabaseH\x00R\x0bgetDatabase\x12\x36\n\tget_table\x18\x08 \x01(\x0b\x32\x17.spark.connect.GetTableH\x00R\x08getTable\x12?\n\x0cget_function\x18\t \x01(\x0b\x32\x1a.spark.connect.GetFunctionH\x00R\x0bgetFunction\x12H\n\x0f\x64\x61tabase_exists\x18\n \x01(\x0b\x32\x1d.spark.connect.DatabaseExistsH\x00R\x0e\x64\x61tabaseExists\x12?\n\x0ctable_exists\x18\x0b \x01(\x0b\x32\x1a.spark.connect.TableExistsH\x00R\x0btableExists\x12H\n\x0f\x66unction_exists\x18\x0c \x01(\x0b\x32\x1d.spark.connect.FunctionExistsH\x00R\x0e\x66unctionExists\x12X\n\x15\x63reate_external_table\x18\r \x01(\x0b\x32".spark.connect.CreateExternalTableH\x00R\x13\x63reateExternalTable\x12?\n\x0c\x63reate_table\x18\x0e \x01(\x0b\x32\x1a.spark.connect.CreateTableH\x00R\x0b\x63reateTable\x12\x43\n\x0e\x64rop_temp_view\x18\x0f \x01(\x0b\x32\x1b.spark.connect.DropTempViewH\x00R\x0c\x64ropTempView\x12V\n\x15\x64rop_global_temp_view\x18\x10 \x01(\x0b\x32!.spark.connect.DropGlobalTempViewH\x00R\x12\x64ropGlobalTempView\x12Q\n\x12recover_partitions\x18\x11 \x01(\x0b\x32 .spark.connect.RecoverPartitionsH\x00R\x11recoverPartitions\x12\x36\n\tis_cached\x18\x12 \x01(\x0b\x32\x17.spark.connect.IsCachedH\x00R\x08isCached\x12<\n\x0b\x63\x61\x63he_table\x18\x13 \x01(\x0b\x32\x19.spark.connect.CacheTableH\x00R\ncacheTable\x12\x42\n\runcache_table\x18\x14 \x01(\x0b\x32\x1b.spark.connect.UncacheTableH\x00R\x0cuncacheTable\x12<\n\x0b\x63lear_cache\x18\x15 \x01(\x0b\x32\x19.spark.connect.ClearCacheH\x00R\nclearCache\x12\x42\n\rrefresh_table\x18\x16 \x01(\x0b\x32\x1b.spark.connect.RefreshTableH\x00R\x0crefreshTable\x12\x46\n\x0frefresh_by_path\x18\x17 \x01(\x0b\x32\x1c.spark.connect.RefreshByPathH\x00R\rrefreshByPath\x12H\n\x0f\x63urrent_catalog\x18\x18 \x01(\x0b\x32\x1d.spark.connect.CurrentCatalogH\x00R\x0e\x63urrentCatalog\x12R\n\x13set_current_catalog\x18\x19 \x01(\x0b\x32 .spark.connect.SetCurrentCatalogH\x00R\x11setCurrentCatalog\x12\x42\n\rlist_catalogs\x18\x1a \x01(\x0b\x32\x1b.spark.connect.ListCatalogsH\x00R\x0clistCatalogsB\n\n\x08\x63\x61t_type"\x11\n\x0f\x43urrentDatabase"-\n\x12SetCurrentDatabase\x12\x17\n\x07\x64\x62_name\x18\x01 \x01(\tR\x06\x64\x62Name"\x0f\n\rListDatabases"6\n\nListTables\x12\x1c\n\x07\x64\x62_name\x18\x01 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"9\n\rListFunctions\x12\x1c\n\x07\x64\x62_name\x18\x01 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"V\n\x0bListColumns\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x1c\n\x07\x64\x62_name\x18\x02 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"&\n\x0bGetDatabase\x12\x17\n\x07\x64\x62_name\x18\x01 \x01(\tR\x06\x64\x62Name"S\n\x08GetTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x1c\n\x07\x64\x62_name\x18\x02 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"\\\n\x0bGetFunction\x12#\n\rfunction_name\x18\x01 \x01(\tR\x0c\x66unctionName\x12\x1c\n\x07\x64\x62_name\x18\x02 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name")\n\x0e\x44\x61tabaseExists\x12\x17\n\x07\x64\x62_name\x18\x01 \x01(\tR\x06\x64\x62Name"V\n\x0bTableExists\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x1c\n\x07\x64\x62_name\x18\x02 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"_\n\x0e\x46unctionExists\x12#\n\rfunction_name\x18\x01 \x01(\tR\x0c\x66unctionName\x12\x1c\n\x07\x64\x62_name\x18\x02 \x01(\tH\x00R\x06\x64\x62Name\x88\x01\x01\x42\n\n\x08_db_name"\xc6\x02\n\x13\x43reateExternalTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x17\n\x04path\x18\x02 \x01(\tH\x00R\x04path\x88\x01\x01\x12\x1b\n\x06source\x18\x03 \x01(\tH\x01R\x06source\x88\x01\x01\x12\x34\n\x06schema\x18\x04 \x01(\x0b\x32\x17.spark.connect.DataTypeH\x02R\x06schema\x88\x01\x01\x12I\n\x07options\x18\x05 \x03(\x0b\x32/.spark.connect.CreateExternalTable.OptionsEntryR\x07options\x1a:\n\x0cOptionsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x14\n\x05value\x18\x02 \x01(\tR\x05value:\x02\x38\x01\x42\x07\n\x05_pathB\t\n\x07_sourceB\t\n\x07_schema"\xed\x02\n\x0b\x43reateTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x17\n\x04path\x18\x02 \x01(\tH\x00R\x04path\x88\x01\x01\x12\x1b\n\x06source\x18\x03 \x01(\tH\x01R\x06source\x88\x01\x01\x12%\n\x0b\x64\x65scription\x18\x04 \x01(\tH\x02R\x0b\x64\x65scription\x88\x01\x01\x12\x34\n\x06schema\x18\x05 \x01(\x0b\x32\x17.spark.connect.DataTypeH\x03R\x06schema\x88\x01\x01\x12\x41\n\x07options\x18\x06 \x03(\x0b\x32\'.spark.connect.CreateTable.OptionsEntryR\x07options\x1a:\n\x0cOptionsEntry\x12\x10\n\x03key\x18\x01 \x01(\tR\x03key\x12\x14\n\x05value\x18\x02 \x01(\tR\x05value:\x02\x38\x01\x42\x07\n\x05_pathB\t\n\x07_sourceB\x0e\n\x0c_descriptionB\t\n\x07_schema"+\n\x0c\x44ropTempView\x12\x1b\n\tview_name\x18\x01 \x01(\tR\x08viewName"1\n\x12\x44ropGlobalTempView\x12\x1b\n\tview_name\x18\x01 \x01(\tR\x08viewName"2\n\x11RecoverPartitions\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName")\n\x08IsCached\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName"\x84\x01\n\nCacheTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName\x12\x45\n\rstorage_level\x18\x02 \x01(\x0b\x32\x1b.spark.connect.StorageLevelH\x00R\x0cstorageLevel\x88\x01\x01\x42\x10\n\x0e_storage_level"-\n\x0cUncacheTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName"\x0c\n\nClearCache"-\n\x0cRefreshTable\x12\x1d\n\ntable_name\x18\x01 \x01(\tR\ttableName"#\n\rRefreshByPath\x12\x12\n\x04path\x18\x01 \x01(\tR\x04path"\x10\n\x0e\x43urrentCatalog"6\n\x11SetCurrentCatalog\x12!\n\x0c\x63\x61talog_name\x18\x01 \x01(\tR\x0b\x63\x61talogName"\x0e\n\x0cListCatalogsB"\n\x1eorg.apache.spark.connect.protoP\x01\x62\x06proto3'
)


_CATALOG = DESCRIPTOR.message_types_by_name["Catalog"]
_CURRENTDATABASE = DESCRIPTOR.message_types_by_name["CurrentDatabase"]
_SETCURRENTDATABASE = DESCRIPTOR.message_types_by_name["SetCurrentDatabase"]
_LISTDATABASES = DESCRIPTOR.message_types_by_name["ListDatabases"]
_LISTTABLES = DESCRIPTOR.message_types_by_name["ListTables"]
_LISTFUNCTIONS = DESCRIPTOR.message_types_by_name["ListFunctions"]
_LISTCOLUMNS = DESCRIPTOR.message_types_by_name["ListColumns"]
_GETDATABASE = DESCRIPTOR.message_types_by_name["GetDatabase"]
_GETTABLE = DESCRIPTOR.message_types_by_name["GetTable"]
_GETFUNCTION = DESCRIPTOR.message_types_by_name["GetFunction"]
_DATABASEEXISTS = DESCRIPTOR.message_types_by_name["DatabaseExists"]
_TABLEEXISTS = DESCRIPTOR.message_types_by_name["TableExists"]
_FUNCTIONEXISTS = DESCRIPTOR.message_types_by_name["FunctionExists"]
_CREATEEXTERNALTABLE = DESCRIPTOR.message_types_by_name["CreateExternalTable"]
_CREATEEXTERNALTABLE_OPTIONSENTRY = _CREATEEXTERNALTABLE.nested_types_by_name["OptionsEntry"]
_CREATETABLE = DESCRIPTOR.message_types_by_name["CreateTable"]
_CREATETABLE_OPTIONSENTRY = _CREATETABLE.nested_types_by_name["OptionsEntry"]
_DROPTEMPVIEW = DESCRIPTOR.message_types_by_name["DropTempView"]
_DROPGLOBALTEMPVIEW = DESCRIPTOR.message_types_by_name["DropGlobalTempView"]
_RECOVERPARTITIONS = DESCRIPTOR.message_types_by_name["RecoverPartitions"]
_ISCACHED = DESCRIPTOR.message_types_by_name["IsCached"]
_CACHETABLE = DESCRIPTOR.message_types_by_name["CacheTable"]
_UNCACHETABLE = DESCRIPTOR.message_types_by_name["UncacheTable"]
_CLEARCACHE = DESCRIPTOR.message_types_by_name["ClearCache"]
_REFRESHTABLE = DESCRIPTOR.message_types_by_name["RefreshTable"]
_REFRESHBYPATH = DESCRIPTOR.message_types_by_name["RefreshByPath"]
_CURRENTCATALOG = DESCRIPTOR.message_types_by_name["CurrentCatalog"]
_SETCURRENTCATALOG = DESCRIPTOR.message_types_by_name["SetCurrentCatalog"]
_LISTCATALOGS = DESCRIPTOR.message_types_by_name["ListCatalogs"]
Catalog = _reflection.GeneratedProtocolMessageType(
    "Catalog",
    (_message.Message,),
    {
        "DESCRIPTOR": _CATALOG,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.Catalog)
    },
)
_sym_db.RegisterMessage(Catalog)

CurrentDatabase = _reflection.GeneratedProtocolMessageType(
    "CurrentDatabase",
    (_message.Message,),
    {
        "DESCRIPTOR": _CURRENTDATABASE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.CurrentDatabase)
    },
)
_sym_db.RegisterMessage(CurrentDatabase)

SetCurrentDatabase = _reflection.GeneratedProtocolMessageType(
    "SetCurrentDatabase",
    (_message.Message,),
    {
        "DESCRIPTOR": _SETCURRENTDATABASE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.SetCurrentDatabase)
    },
)
_sym_db.RegisterMessage(SetCurrentDatabase)

ListDatabases = _reflection.GeneratedProtocolMessageType(
    "ListDatabases",
    (_message.Message,),
    {
        "DESCRIPTOR": _LISTDATABASES,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ListDatabases)
    },
)
_sym_db.RegisterMessage(ListDatabases)

ListTables = _reflection.GeneratedProtocolMessageType(
    "ListTables",
    (_message.Message,),
    {
        "DESCRIPTOR": _LISTTABLES,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ListTables)
    },
)
_sym_db.RegisterMessage(ListTables)

ListFunctions = _reflection.GeneratedProtocolMessageType(
    "ListFunctions",
    (_message.Message,),
    {
        "DESCRIPTOR": _LISTFUNCTIONS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ListFunctions)
    },
)
_sym_db.RegisterMessage(ListFunctions)

ListColumns = _reflection.GeneratedProtocolMessageType(
    "ListColumns",
    (_message.Message,),
    {
        "DESCRIPTOR": _LISTCOLUMNS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ListColumns)
    },
)
_sym_db.RegisterMessage(ListColumns)

GetDatabase = _reflection.GeneratedProtocolMessageType(
    "GetDatabase",
    (_message.Message,),
    {
        "DESCRIPTOR": _GETDATABASE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.GetDatabase)
    },
)
_sym_db.RegisterMessage(GetDatabase)

GetTable = _reflection.GeneratedProtocolMessageType(
    "GetTable",
    (_message.Message,),
    {
        "DESCRIPTOR": _GETTABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.GetTable)
    },
)
_sym_db.RegisterMessage(GetTable)

GetFunction = _reflection.GeneratedProtocolMessageType(
    "GetFunction",
    (_message.Message,),
    {
        "DESCRIPTOR": _GETFUNCTION,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.GetFunction)
    },
)
_sym_db.RegisterMessage(GetFunction)

DatabaseExists = _reflection.GeneratedProtocolMessageType(
    "DatabaseExists",
    (_message.Message,),
    {
        "DESCRIPTOR": _DATABASEEXISTS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.DatabaseExists)
    },
)
_sym_db.RegisterMessage(DatabaseExists)

TableExists = _reflection.GeneratedProtocolMessageType(
    "TableExists",
    (_message.Message,),
    {
        "DESCRIPTOR": _TABLEEXISTS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.TableExists)
    },
)
_sym_db.RegisterMessage(TableExists)

FunctionExists = _reflection.GeneratedProtocolMessageType(
    "FunctionExists",
    (_message.Message,),
    {
        "DESCRIPTOR": _FUNCTIONEXISTS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.FunctionExists)
    },
)
_sym_db.RegisterMessage(FunctionExists)

CreateExternalTable = _reflection.GeneratedProtocolMessageType(
    "CreateExternalTable",
    (_message.Message,),
    {
        "OptionsEntry": _reflection.GeneratedProtocolMessageType(
            "OptionsEntry",
            (_message.Message,),
            {
                "DESCRIPTOR": _CREATEEXTERNALTABLE_OPTIONSENTRY,
                "__module__": "spark.connect.catalog_pb2"
                # @@protoc_insertion_point(class_scope:spark.connect.CreateExternalTable.OptionsEntry)
            },
        ),
        "DESCRIPTOR": _CREATEEXTERNALTABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.CreateExternalTable)
    },
)
_sym_db.RegisterMessage(CreateExternalTable)
_sym_db.RegisterMessage(CreateExternalTable.OptionsEntry)

CreateTable = _reflection.GeneratedProtocolMessageType(
    "CreateTable",
    (_message.Message,),
    {
        "OptionsEntry": _reflection.GeneratedProtocolMessageType(
            "OptionsEntry",
            (_message.Message,),
            {
                "DESCRIPTOR": _CREATETABLE_OPTIONSENTRY,
                "__module__": "spark.connect.catalog_pb2"
                # @@protoc_insertion_point(class_scope:spark.connect.CreateTable.OptionsEntry)
            },
        ),
        "DESCRIPTOR": _CREATETABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.CreateTable)
    },
)
_sym_db.RegisterMessage(CreateTable)
_sym_db.RegisterMessage(CreateTable.OptionsEntry)

DropTempView = _reflection.GeneratedProtocolMessageType(
    "DropTempView",
    (_message.Message,),
    {
        "DESCRIPTOR": _DROPTEMPVIEW,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.DropTempView)
    },
)
_sym_db.RegisterMessage(DropTempView)

DropGlobalTempView = _reflection.GeneratedProtocolMessageType(
    "DropGlobalTempView",
    (_message.Message,),
    {
        "DESCRIPTOR": _DROPGLOBALTEMPVIEW,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.DropGlobalTempView)
    },
)
_sym_db.RegisterMessage(DropGlobalTempView)

RecoverPartitions = _reflection.GeneratedProtocolMessageType(
    "RecoverPartitions",
    (_message.Message,),
    {
        "DESCRIPTOR": _RECOVERPARTITIONS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.RecoverPartitions)
    },
)
_sym_db.RegisterMessage(RecoverPartitions)

IsCached = _reflection.GeneratedProtocolMessageType(
    "IsCached",
    (_message.Message,),
    {
        "DESCRIPTOR": _ISCACHED,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.IsCached)
    },
)
_sym_db.RegisterMessage(IsCached)

CacheTable = _reflection.GeneratedProtocolMessageType(
    "CacheTable",
    (_message.Message,),
    {
        "DESCRIPTOR": _CACHETABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.CacheTable)
    },
)
_sym_db.RegisterMessage(CacheTable)

UncacheTable = _reflection.GeneratedProtocolMessageType(
    "UncacheTable",
    (_message.Message,),
    {
        "DESCRIPTOR": _UNCACHETABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.UncacheTable)
    },
)
_sym_db.RegisterMessage(UncacheTable)

ClearCache = _reflection.GeneratedProtocolMessageType(
    "ClearCache",
    (_message.Message,),
    {
        "DESCRIPTOR": _CLEARCACHE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ClearCache)
    },
)
_sym_db.RegisterMessage(ClearCache)

RefreshTable = _reflection.GeneratedProtocolMessageType(
    "RefreshTable",
    (_message.Message,),
    {
        "DESCRIPTOR": _REFRESHTABLE,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.RefreshTable)
    },
)
_sym_db.RegisterMessage(RefreshTable)

RefreshByPath = _reflection.GeneratedProtocolMessageType(
    "RefreshByPath",
    (_message.Message,),
    {
        "DESCRIPTOR": _REFRESHBYPATH,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.RefreshByPath)
    },
)
_sym_db.RegisterMessage(RefreshByPath)

CurrentCatalog = _reflection.GeneratedProtocolMessageType(
    "CurrentCatalog",
    (_message.Message,),
    {
        "DESCRIPTOR": _CURRENTCATALOG,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.CurrentCatalog)
    },
)
_sym_db.RegisterMessage(CurrentCatalog)

SetCurrentCatalog = _reflection.GeneratedProtocolMessageType(
    "SetCurrentCatalog",
    (_message.Message,),
    {
        "DESCRIPTOR": _SETCURRENTCATALOG,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.SetCurrentCatalog)
    },
)
_sym_db.RegisterMessage(SetCurrentCatalog)

ListCatalogs = _reflection.GeneratedProtocolMessageType(
    "ListCatalogs",
    (_message.Message,),
    {
        "DESCRIPTOR": _LISTCATALOGS,
        "__module__": "spark.connect.catalog_pb2"
        # @@protoc_insertion_point(class_scope:spark.connect.ListCatalogs)
    },
)
_sym_db.RegisterMessage(ListCatalogs)

if _descriptor._USE_C_DESCRIPTORS == False:

    DESCRIPTOR._options = None
    DESCRIPTOR._serialized_options = b"\n\036org.apache.spark.connect.protoP\001"
    _CREATEEXTERNALTABLE_OPTIONSENTRY._options = None
    _CREATEEXTERNALTABLE_OPTIONSENTRY._serialized_options = b"8\001"
    _CREATETABLE_OPTIONSENTRY._options = None
    _CREATETABLE_OPTIONSENTRY._serialized_options = b"8\001"
    _CATALOG._serialized_start = 102
    _CATALOG._serialized_end = 1964
    _CURRENTDATABASE._serialized_start = 1966
    _CURRENTDATABASE._serialized_end = 1983
    _SETCURRENTDATABASE._serialized_start = 1985
    _SETCURRENTDATABASE._serialized_end = 2030
    _LISTDATABASES._serialized_start = 2032
    _LISTDATABASES._serialized_end = 2047
    _LISTTABLES._serialized_start = 2049
    _LISTTABLES._serialized_end = 2103
    _LISTFUNCTIONS._serialized_start = 2105
    _LISTFUNCTIONS._serialized_end = 2162
    _LISTCOLUMNS._serialized_start = 2164
    _LISTCOLUMNS._serialized_end = 2250
    _GETDATABASE._serialized_start = 2252
    _GETDATABASE._serialized_end = 2290
    _GETTABLE._serialized_start = 2292
    _GETTABLE._serialized_end = 2375
    _GETFUNCTION._serialized_start = 2377
    _GETFUNCTION._serialized_end = 2469
    _DATABASEEXISTS._serialized_start = 2471
    _DATABASEEXISTS._serialized_end = 2512
    _TABLEEXISTS._serialized_start = 2514
    _TABLEEXISTS._serialized_end = 2600
    _FUNCTIONEXISTS._serialized_start = 2602
    _FUNCTIONEXISTS._serialized_end = 2697
    _CREATEEXTERNALTABLE._serialized_start = 2700
    _CREATEEXTERNALTABLE._serialized_end = 3026
    _CREATEEXTERNALTABLE_OPTIONSENTRY._serialized_start = 2937
    _CREATEEXTERNALTABLE_OPTIONSENTRY._serialized_end = 2995
    _CREATETABLE._serialized_start = 3029
    _CREATETABLE._serialized_end = 3394
    _CREATETABLE_OPTIONSENTRY._serialized_start = 2937
    _CREATETABLE_OPTIONSENTRY._serialized_end = 2995
    _DROPTEMPVIEW._serialized_start = 3396
    _DROPTEMPVIEW._serialized_end = 3439
    _DROPGLOBALTEMPVIEW._serialized_start = 3441
    _DROPGLOBALTEMPVIEW._serialized_end = 3490
    _RECOVERPARTITIONS._serialized_start = 3492
    _RECOVERPARTITIONS._serialized_end = 3542
    _ISCACHED._serialized_start = 3544
    _ISCACHED._serialized_end = 3585
    _CACHETABLE._serialized_start = 3588
    _CACHETABLE._serialized_end = 3720
    _UNCACHETABLE._serialized_start = 3722
    _UNCACHETABLE._serialized_end = 3767
    _CLEARCACHE._serialized_start = 3769
    _CLEARCACHE._serialized_end = 3781
    _REFRESHTABLE._serialized_start = 3783
    _REFRESHTABLE._serialized_end = 3828
    _REFRESHBYPATH._serialized_start = 3830
    _REFRESHBYPATH._serialized_end = 3865
    _CURRENTCATALOG._serialized_start = 3867
    _CURRENTCATALOG._serialized_end = 3883
    _SETCURRENTCATALOG._serialized_start = 3885
    _SETCURRENTCATALOG._serialized_end = 3939
    _LISTCATALOGS._serialized_start = 3941
    _LISTCATALOGS._serialized_end = 3955
# @@protoc_insertion_point(module_scope)
