/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.spark.sql.thriftserver.cli.thrift;

import org.apache.thrift.scheme.IScheme;
import org.apache.thrift.scheme.SchemeFactory;
import org.apache.thrift.scheme.StandardScheme;

import org.apache.thrift.scheme.TupleScheme;
import org.apache.thrift.protocol.TTupleProtocol;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.EncodingUtils;
import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.server.AbstractNonblockingServer.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.EnumMap;
import java.util.Set;
import java.util.HashSet;
import java.util.EnumSet;
import java.util.Collections;
import java.util.BitSet;
import java.nio.ByteBuffer;
import java.util.Arrays;
import javax.annotation.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"cast", "rawtypes", "serial", "unchecked"})
@Generated(value = "Autogenerated by Thrift Compiler (0.9.3)")
public class TGetQueryIdReq implements org.apache.thrift.TBase<TGetQueryIdReq, TGetQueryIdReq._Fields>, java.io.Serializable, Cloneable, Comparable<TGetQueryIdReq> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TGetQueryIdReq");

  private static final org.apache.thrift.protocol.TField OPERATION_HANDLE_FIELD_DESC = new org.apache.thrift.protocol.TField("operationHandle", org.apache.thrift.protocol.TType.STRUCT, (short)1);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TGetQueryIdReqStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TGetQueryIdReqTupleSchemeFactory());
  }

  private TOperationHandle operationHandle; // required

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    OPERATION_HANDLE((short)1, "operationHandle");

    private static final Map<String, _Fields> byName = new HashMap<String, _Fields>();

    static {
      for (_Fields field : EnumSet.allOf(_Fields.class)) {
        byName.put(field.getFieldName(), field);
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, or null if its not found.
     */
    public static _Fields findByThriftId(int fieldId) {
      switch(fieldId) {
        case 1: // OPERATION_HANDLE
          return OPERATION_HANDLE;
        default:
          return null;
      }
    }

    /**
     * Find the _Fields constant that matches fieldId, throwing an exception
     * if it is not found.
     */
    public static _Fields findByThriftIdOrThrow(int fieldId) {
      _Fields fields = findByThriftId(fieldId);
      if (fields == null) throw new IllegalArgumentException("Field " + fieldId + " doesn't exist!");
      return fields;
    }

    /**
     * Find the _Fields constant that matches name, or null if its not found.
     */
    public static _Fields findByName(String name) {
      return byName.get(name);
    }

    private final short _thriftId;
    private final String _fieldName;

    _Fields(short thriftId, String fieldName) {
      _thriftId = thriftId;
      _fieldName = fieldName;
    }

    public short getThriftFieldId() {
      return _thriftId;
    }

    public String getFieldName() {
      return _fieldName;
    }
  }

  // isset id assignments
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.OPERATION_HANDLE, new org.apache.thrift.meta_data.FieldMetaData("operationHandle", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TOperationHandle.class)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TGetQueryIdReq.class, metaDataMap);
  }

  public TGetQueryIdReq() {
  }

  public TGetQueryIdReq(
    TOperationHandle operationHandle)
  {
    this();
    this.operationHandle = operationHandle;
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TGetQueryIdReq(TGetQueryIdReq other) {
    if (other.isSetOperationHandle()) {
      this.operationHandle = new TOperationHandle(other.operationHandle);
    }
  }

  public TGetQueryIdReq deepCopy() {
    return new TGetQueryIdReq(this);
  }

  @Override
  public void clear() {
    this.operationHandle = null;
  }

  public TOperationHandle getOperationHandle() {
    return this.operationHandle;
  }

  public void setOperationHandle(TOperationHandle operationHandle) {
    this.operationHandle = operationHandle;
  }

  public void unsetOperationHandle() {
    this.operationHandle = null;
  }

  /** Returns true if field operationHandle is set (has been assigned a value) and false otherwise */
  public boolean isSetOperationHandle() {
    return this.operationHandle != null;
  }

  public void setOperationHandleIsSet(boolean value) {
    if (!value) {
      this.operationHandle = null;
    }
  }

  public void setFieldValue(_Fields field, Object value) {
    switch (field) {
    case OPERATION_HANDLE:
      if (value == null) {
        unsetOperationHandle();
      } else {
        setOperationHandle((TOperationHandle)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case OPERATION_HANDLE:
      return getOperationHandle();

    }
    throw new IllegalStateException();
  }

  /** Returns true if field corresponding to fieldID is set (has been assigned a value) and false otherwise */
  public boolean isSet(_Fields field) {
    if (field == null) {
      throw new IllegalArgumentException();
    }

    switch (field) {
    case OPERATION_HANDLE:
      return isSetOperationHandle();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TGetQueryIdReq)
      return this.equals((TGetQueryIdReq)that);
    return false;
  }

  public boolean equals(TGetQueryIdReq that) {
    if (that == null)
      return false;

    boolean this_present_operationHandle = true && this.isSetOperationHandle();
    boolean that_present_operationHandle = true && that.isSetOperationHandle();
    if (this_present_operationHandle || that_present_operationHandle) {
      if (!(this_present_operationHandle && that_present_operationHandle))
        return false;
      if (!this.operationHandle.equals(that.operationHandle))
        return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    List<Object> list = new ArrayList<Object>();

    boolean present_operationHandle = true && (isSetOperationHandle());
    list.add(present_operationHandle);
    if (present_operationHandle)
      list.add(operationHandle);

    return list.hashCode();
  }

  @Override
  public int compareTo(TGetQueryIdReq other) {
    if (!getClass().equals(other.getClass())) {
      return getClass().getName().compareTo(other.getClass().getName());
    }

    int lastComparison = 0;

    lastComparison = Boolean.valueOf(isSetOperationHandle()).compareTo(other.isSetOperationHandle());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOperationHandle()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.operationHandle, other.operationHandle);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    return 0;
  }

  public _Fields fieldForId(int fieldId) {
    return _Fields.findByThriftId(fieldId);
  }

  public void read(org.apache.thrift.protocol.TProtocol iprot) throws org.apache.thrift.TException {
    schemes.get(iprot.getScheme()).getScheme().read(iprot, this);
  }

  public void write(org.apache.thrift.protocol.TProtocol oprot) throws org.apache.thrift.TException {
    schemes.get(oprot.getScheme()).getScheme().write(oprot, this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TGetQueryIdReq(");
    boolean first = true;

    sb.append("operationHandle:");
    if (this.operationHandle == null) {
      sb.append("null");
    } else {
      sb.append(this.operationHandle);
    }
    first = false;
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (!isSetOperationHandle()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'operationHandle' is unset! Struct:" + toString());
    }

    // check for sub-struct validity
    if (operationHandle != null) {
      operationHandle.validate();
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws java.io.IOException {
    try {
      write(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(out)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    try {
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TGetQueryIdReqStandardSchemeFactory implements SchemeFactory {
    public TGetQueryIdReqStandardScheme getScheme() {
      return new TGetQueryIdReqStandardScheme();
    }
  }

  private static class TGetQueryIdReqStandardScheme extends StandardScheme<TGetQueryIdReq> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TGetQueryIdReq struct) throws org.apache.thrift.TException {
      org.apache.thrift.protocol.TField schemeField;
      iprot.readStructBegin();
      while (true)
      {
        schemeField = iprot.readFieldBegin();
        if (schemeField.type == org.apache.thrift.protocol.TType.STOP) { 
          break;
        }
        switch (schemeField.id) {
          case 1: // OPERATION_HANDLE
            if (schemeField.type == org.apache.thrift.protocol.TType.STRUCT) {
              struct.operationHandle = new TOperationHandle();
              struct.operationHandle.read(iprot);
              struct.setOperationHandleIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          default:
            org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
        }
        iprot.readFieldEnd();
      }
      iprot.readStructEnd();
      struct.validate();
    }

    public void write(org.apache.thrift.protocol.TProtocol oprot, TGetQueryIdReq struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.operationHandle != null) {
        oprot.writeFieldBegin(OPERATION_HANDLE_FIELD_DESC);
        struct.operationHandle.write(oprot);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TGetQueryIdReqTupleSchemeFactory implements SchemeFactory {
    public TGetQueryIdReqTupleScheme getScheme() {
      return new TGetQueryIdReqTupleScheme();
    }
  }

  private static class TGetQueryIdReqTupleScheme extends TupleScheme<TGetQueryIdReq> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TGetQueryIdReq struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      struct.operationHandle.write(oprot);
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TGetQueryIdReq struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.operationHandle = new TOperationHandle();
      struct.operationHandle.read(iprot);
      struct.setOperationHandleIsSet(true);
    }
  }

}

