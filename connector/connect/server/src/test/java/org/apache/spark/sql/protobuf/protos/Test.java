// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: test.proto

package org.apache.spark.sql.protobuf.protos;

/**
 * SPARK-43646: This is generated by the `connector/connect/server/src/test/resources/test.proto`
 * to test the spark protobuf uber jar. If need to regenerate this file:
 * 1. Modify `connector/connect/server/src/test/resources/test.proto` to generate Java files
 * and replace the current file.
 * 2. Replace all `com.google.protobuf` in the file with `org.sparkproject.spark_protobuf.protobuf.`
 */
public final class Test {
  private Test() {}
  public static void registerAllExtensions(
      org.sparkproject.spark_protobuf.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      org.sparkproject.spark_protobuf.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (org.sparkproject.spark_protobuf.protobuf.ExtensionRegistryLite) registry);
  }
  static final org.sparkproject.spark_protobuf.protobuf.Descriptors.Descriptor
    internal_static_org_apache_spark_sql_protobuf_protos_TestObj_descriptor;
  static final 
    org.sparkproject.spark_protobuf.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_org_apache_spark_sql_protobuf_protos_TestObj_fieldAccessorTable;

  public static org.sparkproject.spark_protobuf.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  org.sparkproject.spark_protobuf.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    String[] descriptorData = {
      "\n\ntest.proto\022$org.apache.spark.sql.proto" +
      "buf.protos\"!\n\007TestObj\022\n\n\002v1\030\001 \001(\003\022\n\n\002v2\030" +
      "\002 \001(\005B\002P\001b\006proto3"
    };
    descriptor = org.sparkproject.spark_protobuf.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new org.sparkproject.spark_protobuf.protobuf.Descriptors.FileDescriptor[] {
        });
    internal_static_org_apache_spark_sql_protobuf_protos_TestObj_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_org_apache_spark_sql_protobuf_protos_TestObj_fieldAccessorTable = new
      org.sparkproject.spark_protobuf.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_org_apache_spark_sql_protobuf_protos_TestObj_descriptor,
        new String[] { "V1", "V2", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
