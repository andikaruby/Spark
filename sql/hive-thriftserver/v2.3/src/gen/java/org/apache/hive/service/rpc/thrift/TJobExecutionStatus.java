/**
 * Autogenerated by Thrift Compiler (0.9.3)
 *
 * DO NOT EDIT UNLESS YOU ARE SURE THAT YOU KNOW WHAT YOU ARE DOING
 *  @generated
 */
package org.apache.hive.service.rpc.thrift;


import java.util.Map;
import java.util.HashMap;
import org.apache.thrift.TEnum;

public enum TJobExecutionStatus implements org.apache.thrift.TEnum {
  IN_PROGRESS(0),
  COMPLETE(1),
  NOT_AVAILABLE(2);

  private final int value;

  private TJobExecutionStatus(int value) {
    this.value = value;
  }

  /**
   * Get the integer value of this enum value, as defined in the Thrift IDL.
   */
  public int getValue() {
    return value;
  }

  /**
   * Find a the enum type by its integer value, as defined in the Thrift IDL.
   * @return null if the value is not found.
   */
  public static TJobExecutionStatus findByValue(int value) { 
    switch (value) {
      case 0:
        return IN_PROGRESS;
      case 1:
        return COMPLETE;
      case 2:
        return NOT_AVAILABLE;
      default:
        return null;
    }
  }
}
