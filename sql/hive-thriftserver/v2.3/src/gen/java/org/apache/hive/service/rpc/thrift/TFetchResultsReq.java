/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.hive.service.rpc.thrift;

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
public class TFetchResultsReq implements org.apache.thrift.TBase<TFetchResultsReq, TFetchResultsReq._Fields>, java.io.Serializable, Cloneable, Comparable<TFetchResultsReq> {
  private static final org.apache.thrift.protocol.TStruct STRUCT_DESC = new org.apache.thrift.protocol.TStruct("TFetchResultsReq");

  private static final org.apache.thrift.protocol.TField OPERATION_HANDLE_FIELD_DESC = new org.apache.thrift.protocol.TField("operationHandle", org.apache.thrift.protocol.TType.STRUCT, (short)1);
  private static final org.apache.thrift.protocol.TField ORIENTATION_FIELD_DESC = new org.apache.thrift.protocol.TField("orientation", org.apache.thrift.protocol.TType.I32, (short)2);
  private static final org.apache.thrift.protocol.TField MAX_ROWS_FIELD_DESC = new org.apache.thrift.protocol.TField("maxRows", org.apache.thrift.protocol.TType.I64, (short)3);
  private static final org.apache.thrift.protocol.TField FETCH_TYPE_FIELD_DESC = new org.apache.thrift.protocol.TField("fetchType", org.apache.thrift.protocol.TType.I16, (short)4);

  private static final Map<Class<? extends IScheme>, SchemeFactory> schemes = new HashMap<Class<? extends IScheme>, SchemeFactory>();
  static {
    schemes.put(StandardScheme.class, new TFetchResultsReqStandardSchemeFactory());
    schemes.put(TupleScheme.class, new TFetchResultsReqTupleSchemeFactory());
  }

  private TOperationHandle operationHandle; // required
  private TFetchOrientation orientation; // required
  private long maxRows; // required
  private short fetchType; // optional

  /** The set of fields this struct contains, along with convenience methods for finding and manipulating them. */
  public enum _Fields implements org.apache.thrift.TFieldIdEnum {
    OPERATION_HANDLE((short)1, "operationHandle"),
    /**
     * 
     * @see TFetchOrientation
     */
    ORIENTATION((short)2, "orientation"),
    MAX_ROWS((short)3, "maxRows"),
    FETCH_TYPE((short)4, "fetchType");

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
        case 2: // ORIENTATION
          return ORIENTATION;
        case 3: // MAX_ROWS
          return MAX_ROWS;
        case 4: // FETCH_TYPE
          return FETCH_TYPE;
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
  private static final int __MAXROWS_ISSET_ID = 0;
  private static final int __FETCHTYPE_ISSET_ID = 1;
  private byte __isset_bitfield = 0;
  private static final _Fields optionals[] = {_Fields.FETCH_TYPE};
  public static final Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> metaDataMap;
  static {
    Map<_Fields, org.apache.thrift.meta_data.FieldMetaData> tmpMap = new EnumMap<_Fields, org.apache.thrift.meta_data.FieldMetaData>(_Fields.class);
    tmpMap.put(_Fields.OPERATION_HANDLE, new org.apache.thrift.meta_data.FieldMetaData("operationHandle", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.StructMetaData(org.apache.thrift.protocol.TType.STRUCT, TOperationHandle.class)));
    tmpMap.put(_Fields.ORIENTATION, new org.apache.thrift.meta_data.FieldMetaData("orientation", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.EnumMetaData(org.apache.thrift.protocol.TType.ENUM, TFetchOrientation.class)));
    tmpMap.put(_Fields.MAX_ROWS, new org.apache.thrift.meta_data.FieldMetaData("maxRows", org.apache.thrift.TFieldRequirementType.REQUIRED, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I64)));
    tmpMap.put(_Fields.FETCH_TYPE, new org.apache.thrift.meta_data.FieldMetaData("fetchType", org.apache.thrift.TFieldRequirementType.OPTIONAL, 
        new org.apache.thrift.meta_data.FieldValueMetaData(org.apache.thrift.protocol.TType.I16)));
    metaDataMap = Collections.unmodifiableMap(tmpMap);
    org.apache.thrift.meta_data.FieldMetaData.addStructMetaDataMap(TFetchResultsReq.class, metaDataMap);
  }

  public TFetchResultsReq() {
    this.orientation = org.apache.hive.service.rpc.thrift.TFetchOrientation.FETCH_NEXT;

    this.fetchType = (short)0;

  }

  public TFetchResultsReq(
    TOperationHandle operationHandle,
    TFetchOrientation orientation,
    long maxRows)
  {
    this();
    this.operationHandle = operationHandle;
    this.orientation = orientation;
    this.maxRows = maxRows;
    setMaxRowsIsSet(true);
  }

  /**
   * Performs a deep copy on <i>other</i>.
   */
  public TFetchResultsReq(TFetchResultsReq other) {
    __isset_bitfield = other.__isset_bitfield;
    if (other.isSetOperationHandle()) {
      this.operationHandle = new TOperationHandle(other.operationHandle);
    }
    if (other.isSetOrientation()) {
      this.orientation = other.orientation;
    }
    this.maxRows = other.maxRows;
    this.fetchType = other.fetchType;
  }

  public TFetchResultsReq deepCopy() {
    return new TFetchResultsReq(this);
  }

  @Override
  public void clear() {
    this.operationHandle = null;
    this.orientation = org.apache.hive.service.rpc.thrift.TFetchOrientation.FETCH_NEXT;

    setMaxRowsIsSet(false);
    this.maxRows = 0;
    this.fetchType = (short)0;

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

  /**
   * 
   * @see TFetchOrientation
   */
  public TFetchOrientation getOrientation() {
    return this.orientation;
  }

  /**
   * 
   * @see TFetchOrientation
   */
  public void setOrientation(TFetchOrientation orientation) {
    this.orientation = orientation;
  }

  public void unsetOrientation() {
    this.orientation = null;
  }

  /** Returns true if field orientation is set (has been assigned a value) and false otherwise */
  public boolean isSetOrientation() {
    return this.orientation != null;
  }

  public void setOrientationIsSet(boolean value) {
    if (!value) {
      this.orientation = null;
    }
  }

  public long getMaxRows() {
    return this.maxRows;
  }

  public void setMaxRows(long maxRows) {
    this.maxRows = maxRows;
    setMaxRowsIsSet(true);
  }

  public void unsetMaxRows() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __MAXROWS_ISSET_ID);
  }

  /** Returns true if field maxRows is set (has been assigned a value) and false otherwise */
  public boolean isSetMaxRows() {
    return EncodingUtils.testBit(__isset_bitfield, __MAXROWS_ISSET_ID);
  }

  public void setMaxRowsIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __MAXROWS_ISSET_ID, value);
  }

  public short getFetchType() {
    return this.fetchType;
  }

  public void setFetchType(short fetchType) {
    this.fetchType = fetchType;
    setFetchTypeIsSet(true);
  }

  public void unsetFetchType() {
    __isset_bitfield = EncodingUtils.clearBit(__isset_bitfield, __FETCHTYPE_ISSET_ID);
  }

  /** Returns true if field fetchType is set (has been assigned a value) and false otherwise */
  public boolean isSetFetchType() {
    return EncodingUtils.testBit(__isset_bitfield, __FETCHTYPE_ISSET_ID);
  }

  public void setFetchTypeIsSet(boolean value) {
    __isset_bitfield = EncodingUtils.setBit(__isset_bitfield, __FETCHTYPE_ISSET_ID, value);
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

    case ORIENTATION:
      if (value == null) {
        unsetOrientation();
      } else {
        setOrientation((TFetchOrientation)value);
      }
      break;

    case MAX_ROWS:
      if (value == null) {
        unsetMaxRows();
      } else {
        setMaxRows((Long)value);
      }
      break;

    case FETCH_TYPE:
      if (value == null) {
        unsetFetchType();
      } else {
        setFetchType((Short)value);
      }
      break;

    }
  }

  public Object getFieldValue(_Fields field) {
    switch (field) {
    case OPERATION_HANDLE:
      return getOperationHandle();

    case ORIENTATION:
      return getOrientation();

    case MAX_ROWS:
      return getMaxRows();

    case FETCH_TYPE:
      return getFetchType();

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
    case ORIENTATION:
      return isSetOrientation();
    case MAX_ROWS:
      return isSetMaxRows();
    case FETCH_TYPE:
      return isSetFetchType();
    }
    throw new IllegalStateException();
  }

  @Override
  public boolean equals(Object that) {
    if (that == null)
      return false;
    if (that instanceof TFetchResultsReq)
      return this.equals((TFetchResultsReq)that);
    return false;
  }

  public boolean equals(TFetchResultsReq that) {
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

    boolean this_present_orientation = true && this.isSetOrientation();
    boolean that_present_orientation = true && that.isSetOrientation();
    if (this_present_orientation || that_present_orientation) {
      if (!(this_present_orientation && that_present_orientation))
        return false;
      if (!this.orientation.equals(that.orientation))
        return false;
    }

    boolean this_present_maxRows = true;
    boolean that_present_maxRows = true;
    if (this_present_maxRows || that_present_maxRows) {
      if (!(this_present_maxRows && that_present_maxRows))
        return false;
      if (this.maxRows != that.maxRows)
        return false;
    }

    boolean this_present_fetchType = true && this.isSetFetchType();
    boolean that_present_fetchType = true && that.isSetFetchType();
    if (this_present_fetchType || that_present_fetchType) {
      if (!(this_present_fetchType && that_present_fetchType))
        return false;
      if (this.fetchType != that.fetchType)
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

    boolean present_orientation = true && (isSetOrientation());
    list.add(present_orientation);
    if (present_orientation)
      list.add(orientation.getValue());

    boolean present_maxRows = true;
    list.add(present_maxRows);
    if (present_maxRows)
      list.add(maxRows);

    boolean present_fetchType = true && (isSetFetchType());
    list.add(present_fetchType);
    if (present_fetchType)
      list.add(fetchType);

    return list.hashCode();
  }

  @Override
  public int compareTo(TFetchResultsReq other) {
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
    lastComparison = Boolean.valueOf(isSetOrientation()).compareTo(other.isSetOrientation());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetOrientation()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.orientation, other.orientation);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetMaxRows()).compareTo(other.isSetMaxRows());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetMaxRows()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.maxRows, other.maxRows);
      if (lastComparison != 0) {
        return lastComparison;
      }
    }
    lastComparison = Boolean.valueOf(isSetFetchType()).compareTo(other.isSetFetchType());
    if (lastComparison != 0) {
      return lastComparison;
    }
    if (isSetFetchType()) {
      lastComparison = org.apache.thrift.TBaseHelper.compareTo(this.fetchType, other.fetchType);
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
    StringBuilder sb = new StringBuilder("TFetchResultsReq(");
    boolean first = true;

    sb.append("operationHandle:");
    if (this.operationHandle == null) {
      sb.append("null");
    } else {
      sb.append(this.operationHandle);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("orientation:");
    if (this.orientation == null) {
      sb.append("null");
    } else {
      sb.append(this.orientation);
    }
    first = false;
    if (!first) sb.append(", ");
    sb.append("maxRows:");
    sb.append(this.maxRows);
    first = false;
    if (isSetFetchType()) {
      if (!first) sb.append(", ");
      sb.append("fetchType:");
      sb.append(this.fetchType);
      first = false;
    }
    sb.append(")");
    return sb.toString();
  }

  public void validate() throws org.apache.thrift.TException {
    // check for required fields
    if (!isSetOperationHandle()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'operationHandle' is unset! Struct:" + toString());
    }

    if (!isSetOrientation()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'orientation' is unset! Struct:" + toString());
    }

    if (!isSetMaxRows()) {
      throw new org.apache.thrift.protocol.TProtocolException("Required field 'maxRows' is unset! Struct:" + toString());
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
      // it doesn't seem like you should have to do this, but java serialization is wacky, and doesn't call the default constructor.
      __isset_bitfield = 0;
      read(new org.apache.thrift.protocol.TCompactProtocol(new org.apache.thrift.transport.TIOStreamTransport(in)));
    } catch (org.apache.thrift.TException te) {
      throw new java.io.IOException(te);
    }
  }

  private static class TFetchResultsReqStandardSchemeFactory implements SchemeFactory {
    public TFetchResultsReqStandardScheme getScheme() {
      return new TFetchResultsReqStandardScheme();
    }
  }

  private static class TFetchResultsReqStandardScheme extends StandardScheme<TFetchResultsReq> {

    public void read(org.apache.thrift.protocol.TProtocol iprot, TFetchResultsReq struct) throws org.apache.thrift.TException {
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
          case 2: // ORIENTATION
            if (schemeField.type == org.apache.thrift.protocol.TType.I32) {
              struct.orientation = org.apache.hive.service.rpc.thrift.TFetchOrientation.findByValue(iprot.readI32());
              struct.setOrientationIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 3: // MAX_ROWS
            if (schemeField.type == org.apache.thrift.protocol.TType.I64) {
              struct.maxRows = iprot.readI64();
              struct.setMaxRowsIsSet(true);
            } else { 
              org.apache.thrift.protocol.TProtocolUtil.skip(iprot, schemeField.type);
            }
            break;
          case 4: // FETCH_TYPE
            if (schemeField.type == org.apache.thrift.protocol.TType.I16) {
              struct.fetchType = iprot.readI16();
              struct.setFetchTypeIsSet(true);
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

    public void write(org.apache.thrift.protocol.TProtocol oprot, TFetchResultsReq struct) throws org.apache.thrift.TException {
      struct.validate();

      oprot.writeStructBegin(STRUCT_DESC);
      if (struct.operationHandle != null) {
        oprot.writeFieldBegin(OPERATION_HANDLE_FIELD_DESC);
        struct.operationHandle.write(oprot);
        oprot.writeFieldEnd();
      }
      if (struct.orientation != null) {
        oprot.writeFieldBegin(ORIENTATION_FIELD_DESC);
        oprot.writeI32(struct.orientation.getValue());
        oprot.writeFieldEnd();
      }
      oprot.writeFieldBegin(MAX_ROWS_FIELD_DESC);
      oprot.writeI64(struct.maxRows);
      oprot.writeFieldEnd();
      if (struct.isSetFetchType()) {
        oprot.writeFieldBegin(FETCH_TYPE_FIELD_DESC);
        oprot.writeI16(struct.fetchType);
        oprot.writeFieldEnd();
      }
      oprot.writeFieldStop();
      oprot.writeStructEnd();
    }

  }

  private static class TFetchResultsReqTupleSchemeFactory implements SchemeFactory {
    public TFetchResultsReqTupleScheme getScheme() {
      return new TFetchResultsReqTupleScheme();
    }
  }

  private static class TFetchResultsReqTupleScheme extends TupleScheme<TFetchResultsReq> {

    @Override
    public void write(org.apache.thrift.protocol.TProtocol prot, TFetchResultsReq struct) throws org.apache.thrift.TException {
      TTupleProtocol oprot = (TTupleProtocol) prot;
      struct.operationHandle.write(oprot);
      oprot.writeI32(struct.orientation.getValue());
      oprot.writeI64(struct.maxRows);
      BitSet optionals = new BitSet();
      if (struct.isSetFetchType()) {
        optionals.set(0);
      }
      oprot.writeBitSet(optionals, 1);
      if (struct.isSetFetchType()) {
        oprot.writeI16(struct.fetchType);
      }
    }

    @Override
    public void read(org.apache.thrift.protocol.TProtocol prot, TFetchResultsReq struct) throws org.apache.thrift.TException {
      TTupleProtocol iprot = (TTupleProtocol) prot;
      struct.operationHandle = new TOperationHandle();
      struct.operationHandle.read(iprot);
      struct.setOperationHandleIsSet(true);
      struct.orientation = org.apache.hive.service.rpc.thrift.TFetchOrientation.findByValue(iprot.readI32());
      struct.setOrientationIsSet(true);
      struct.maxRows = iprot.readI64();
      struct.setMaxRowsIsSet(true);
      BitSet incoming = iprot.readBitSet(1);
      if (incoming.get(0)) {
        struct.fetchType = iprot.readI16();
        struct.setFetchTypeIsSet(true);
      }
    }
  }

}

