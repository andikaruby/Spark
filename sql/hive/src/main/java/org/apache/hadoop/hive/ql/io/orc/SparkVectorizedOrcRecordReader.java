package org.apache.hadoop.hive.ql.io.orc;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.ql.exec.vector.ColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.BytesColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DecimalColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.DoubleColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.LongColumnVector;
import org.apache.hadoop.hive.ql.exec.vector.VectorizedRowBatch;
import org.apache.hadoop.hive.ql.metadata.HiveException;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspectorFactory;
import org.apache.hadoop.hive.serde2.objectinspector.PrimitiveObjectInspector;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.hive.serde2.typeinfo.DecimalTypeInfo;
import org.apache.hadoop.hive.serde2.typeinfo.PrimitiveTypeInfo;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.RecordReader;

/**
 * This is based on 
 * {@link org.apache.hadoop.hive.ql.io.orc.VectorizedOrcInputFormat.VectorizedOrcRecordReader}.
 */
public class SparkVectorizedOrcRecordReader
      implements RecordReader<NullWritable, VectorizedRowBatch> {
    private final org.apache.hadoop.hive.ql.io.orc.RecordReader reader;
    private final long offset;
    private final long length;
    private float progress = 0.0f;
    private ObjectInspector objectInspector;

    SparkVectorizedOrcRecordReader(Reader file, Configuration conf,
        FileSplit fileSplit) throws IOException {
      this.offset = fileSplit.getStart();
      this.length = fileSplit.getLength();
      this.objectInspector = file.getObjectInspector();
      this.reader = OrcInputFormat.createReaderFromFile(file, conf, this.offset,
        this.length);
      this.progress = reader.getProgress();
    }

    /**
     * Create a ColumnVector based on given ObjectInspector's type info.
     *
     * @param inspector ObjectInspector
     */
    private ColumnVector createColumnVector(ObjectInspector inspector) {
      switch(inspector.getCategory()) {
      case PRIMITIVE:
        {
          PrimitiveTypeInfo primitiveTypeInfo =
            (PrimitiveTypeInfo) ((PrimitiveObjectInspector)inspector).getTypeInfo();
          switch(primitiveTypeInfo.getPrimitiveCategory()) {
            case BOOLEAN:
            case BYTE:
            case SHORT:
            case INT:
            case LONG:
            case DATE:
            case INTERVAL_YEAR_MONTH:
              return new LongColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
            case FLOAT:
            case DOUBLE:
              return new DoubleColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
            case BINARY:
            case STRING:
            case CHAR:
            case VARCHAR:
              BytesColumnVector column = new BytesColumnVector(VectorizedRowBatch.DEFAULT_SIZE);
              column.initBuffer();
              return column;
            case DECIMAL:
              DecimalTypeInfo tInfo = (DecimalTypeInfo) primitiveTypeInfo;
              return new DecimalColumnVector(VectorizedRowBatch.DEFAULT_SIZE,
                  tInfo.precision(), tInfo.scale());
            default:
              throw new RuntimeException("Vectorizaton is not supported for datatype:"
                  + primitiveTypeInfo.getPrimitiveCategory());
          }
        }
      default:
        throw new RuntimeException("Vectorization is not supported for datatype:"
            + inspector.getCategory());
      }
    }

    /**
     * Walk through the object inspector and add column vectors
     *
     * @param oi StructObjectInspector
     * @param cvList ColumnVectors are populated in this list
     */
    private void allocateColumnVector(StructObjectInspector oi,
        List<ColumnVector> cvList) throws HiveException {
      if (cvList == null) {
        throw new HiveException("Null columnvector list");
      }
      if (oi == null) {
        return;
      }
      final List<? extends StructField> fields = oi.getAllStructFieldRefs();
      for(StructField field : fields) {
        ObjectInspector fieldObjectInspector = field.getFieldObjectInspector();
        cvList.add(createColumnVector(fieldObjectInspector));
      }
    }

    /**
     * Create VectorizedRowBatch from ObjectInspector
     *
     * @param oi
     * @return
     * @throws HiveException
     */
    private VectorizedRowBatch constructVectorizedRowBatch(
        StructObjectInspector oi) throws HiveException {
      final List<ColumnVector> cvList = new LinkedList<ColumnVector>();
      allocateColumnVector(oi, cvList);
      final VectorizedRowBatch result = new VectorizedRowBatch(cvList.size());
      int i = 0;
      for(ColumnVector cv : cvList) {
        result.cols[i++] = cv;
      }
      return result;
    }

    @Override
    public boolean next(NullWritable key, VectorizedRowBatch value) throws IOException {
      try {
        reader.nextBatch(value);
        if (value == null || value.endOfFile || value.size == 0) {
          return false;
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      progress = reader.getProgress();
      return true;
    }

    @Override
    public NullWritable createKey() {
      return NullWritable.get();
    }

    @Override
    public VectorizedRowBatch createValue() {
      try {
        return constructVectorizedRowBatch((StructObjectInspector)this.objectInspector);
      } catch (HiveException e) {
      }
      return null;
    }

    @Override
    public long getPos() throws IOException {
      return offset + (long) (progress * length);
    }

    @Override
    public void close() throws IOException {
      reader.close();
    }

    @Override
    public float getProgress() throws IOException {
      return progress;
    }
  }  
