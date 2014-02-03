package catalyst
package execution

import scala.collection.JavaConversions._

import org.apache.hadoop.hive.serde2.{io => hiveIo}
import org.apache.hadoop.hive.serde2.objectinspector.primitive._
import org.apache.hadoop.hive.serde2.objectinspector.{ListObjectInspector, StructObjectInspector}
import org.apache.hadoop.hive.serde2.objectinspector.{MapObjectInspector, ObjectInspector}
import org.apache.hadoop.hive.ql.exec.{FunctionInfo, FunctionRegistry}
import org.apache.hadoop.hive.ql.udf.generic.{GenericUDAFEvaluator, AbstractGenericUDAFResolver}
import org.apache.hadoop.hive.ql.udf.generic.GenericUDF
import org.apache.hadoop.hive.ql.exec.UDF
import org.apache.hadoop.{io => hadoopIo}

import catalyst.expressions._
import catalyst.types._

object HiveFunctionRegistry extends analysis.FunctionRegistry with HiveFunctionFactory {
  def lookupFunction(name: String, children: Seq[Expression]): Expression = {
    // We only look it up to see if it exists, but do not include it in the HiveUDF since it is
    // not always serializable.
    val functionInfo: FunctionInfo = Option(FunctionRegistry.getFunctionInfo(name)).getOrElse(
      sys.error(s"Couldn't find function $name"))

    if (classOf[UDF].isAssignableFrom(functionInfo.getFunctionClass)) {
      val function = createFunction[UDF](name)
      val method = function.getResolver.getEvalMethod(children.map(_.dataType.toTypeInfo))

      lazy val expectedDataTypes = method.getParameterTypes.map(javaClassToDataType)

      HiveSimpleUdf(
        name,
        children.zip(expectedDataTypes).map { case (e, t) => Cast(e, t) }
      )
    } else if (classOf[GenericUDF].isAssignableFrom(functionInfo.getFunctionClass)) {
      HiveGenericUdf(name, children)
    } else if (
         classOf[AbstractGenericUDAFResolver].isAssignableFrom(functionInfo.getFunctionClass)) {
      HiveGenericUdaf(name, children)
    } else {
      sys.error(s"No handler for udf ${functionInfo.getFunctionClass}")
    }
  }

  def javaClassToDataType(clz: Class[_]): DataType = clz match {
    case c: Class[_] if c == classOf[hadoopIo.DoubleWritable] => DoubleType
    case c: Class[_] if c == classOf[hiveIo.DoubleWritable] => DoubleType
    case c: Class[_] if c == classOf[hiveIo.HiveDecimalWritable] => DecimalType
    case c: Class[_] if c == classOf[hiveIo.ByteWritable] => ByteType
    case c: Class[_] if c == classOf[hiveIo.ShortWritable] => ShortType
    case c: Class[_] if c == classOf[hadoopIo.Text] => StringType
    case c: Class[_] if c == classOf[hadoopIo.IntWritable] => IntegerType
    case c: Class[_] if c == classOf[hadoopIo.LongWritable] => LongType
    case c: Class[_] if c == classOf[hadoopIo.FloatWritable] => FloatType
    case c: Class[_] if c == classOf[hadoopIo.BooleanWritable] => BooleanType
    case c: Class[_] if c == classOf[java.lang.String] => StringType
    case c: Class[_] if c == java.lang.Short.TYPE => ShortType
    case c: Class[_] if c == java.lang.Integer.TYPE => ShortType
    case c: Class[_] if c == java.lang.Long.TYPE => LongType
    case c: Class[_] if c == java.lang.Double.TYPE => DoubleType
    case c: Class[_] if c == java.lang.Byte.TYPE => ByteType
    case c: Class[_] if c == java.lang.Float.TYPE => FloatType
    case c: Class[_] if c == java.lang.Boolean.TYPE => BooleanType
    case c: Class[_] if c == classOf[java.lang.Short] => ShortType
    case c: Class[_] if c == classOf[java.lang.Integer] => ShortType
    case c: Class[_] if c == classOf[java.lang.Long] => LongType
    case c: Class[_] if c == classOf[java.lang.Double] => DoubleType
    case c: Class[_] if c == classOf[java.lang.Byte] => ByteType
    case c: Class[_] if c == classOf[java.lang.Float] => FloatType
    case c: Class[_] if c == classOf[java.lang.Boolean] => BooleanType
  }
}

trait HiveFunctionFactory {
  def getFunctionInfo(name: String) = FunctionRegistry.getFunctionInfo(name)
  def getFunctionClass(name: String) = getFunctionInfo(name).getFunctionClass
  def createFunction[UDFType](name: String) =
    getFunctionClass(name).newInstance.asInstanceOf[UDFType]

  def unwrap(a: Any): Any = a match {
    case null => null
    case i: hadoopIo.IntWritable => i.get
    case t: hadoopIo.Text => t.toString
    case l: hadoopIo.LongWritable => l.get
    case d: hadoopIo.DoubleWritable => d.get()
    case d: hiveIo.DoubleWritable => d.get
    case s: hiveIo.ShortWritable => s.get
    case b: hadoopIo.BooleanWritable => b.get()
    case b: hiveIo.ByteWritable => b.get
    case list: java.util.List[_] => list.map(unwrap)
    case array: Array[_] => array.map(unwrap)
    case p: java.lang.Short => p
    case p: java.lang.Long => p
    case p: java.lang.Float => p
    case p: java.lang.Integer => p
    case p: java.lang.Double => p
    case p: java.lang.Byte => p
    case p: java.lang.Boolean => p
    case str: String => str
  }
}

abstract class HiveUdf
    extends Expression with ImplementedUdf with Logging with HiveFunctionFactory {
  self: Product =>

  type UDFType
  val name: String

  def nullable = true
  def references = children.flatMap(_.references).toSet

  // FunctionInfo is not serializable so we must look it up here again.
  lazy val functionInfo = getFunctionInfo(name)
  lazy val function = createFunction[UDFType](name)

  override def toString = s"${nodeName}#${functionInfo.getDisplayName}(${children.mkString(",")})"
}

case class HiveSimpleUdf(name: String, children: Seq[Expression]) extends HiveUdf {
  import HiveFunctionRegistry._
  type UDFType = UDF

  @transient
  lazy val method = function.getResolver.getEvalMethod(children.map(_.dataType.toTypeInfo))
  @transient
  lazy val dataType = javaClassToDataType(method.getReturnType)

  lazy val wrappers: Array[(Any) => AnyRef] = method.getParameterTypes.map { argClass =>
    val primitiveClasses = Seq(
      Integer.TYPE, classOf[java.lang.Integer], classOf[java.lang.String], java.lang.Double.TYPE,
      classOf[java.lang.Double], java.lang.Long.TYPE, classOf[java.lang.Long]
    )
    val matchingConstructor = argClass.getConstructors.find { c =>
      c.getParameterTypes.size == 1 && primitiveClasses.contains(c.getParameterTypes.head)
    }

    val constructor = matchingConstructor.getOrElse(
      sys.error(s"No matching wrapper found, options: ${argClass.getConstructors.toSeq}."))

    (a: Any) => {
      logger.debug(
        s"Wrapping $a of type ${if (a == null) "null" else a.getClass.getName} using $constructor.")
      // We must make sure that primitives get boxed java style.
      if (a == null) {
        null
      } else {
        constructor.newInstance(a match {
          case i: Int => i: java.lang.Integer
          case other: AnyRef => other
        }).asInstanceOf[AnyRef]
      }
    }
  }

  // TODO: Finish input output types.
  def evaluate(evaluatedChildren: Seq[Any]): Any = {
    // Wrap the function arguments in the expected types.
    val args = evaluatedChildren.zip(wrappers).map {
      case (arg, wrapper) => wrapper(arg)
    }

    // Invoke the udf and unwrap the result.
    unwrap(method.invoke(function, args: _*))
  }
}

case class HiveGenericUdf(
    name: String,
    children: Seq[Expression]) extends HiveUdf with HiveInspectors {
  import org.apache.hadoop.hive.ql.udf.generic.GenericUDF._
  type UDFType = GenericUDF

  lazy val inspectors: Seq[AbstractPrimitiveJavaObjectInspector] = children.map(_.dataType).map {
    case StringType => PrimitiveObjectInspectorFactory.javaStringObjectInspector
    case IntegerType => PrimitiveObjectInspectorFactory.javaIntObjectInspector
    case DoubleType => PrimitiveObjectInspectorFactory.javaDoubleObjectInspector
    case BooleanType => PrimitiveObjectInspectorFactory.javaBooleanObjectInspector
    case LongType => PrimitiveObjectInspectorFactory.javaLongObjectInspector
    case ShortType => PrimitiveObjectInspectorFactory.javaShortObjectInspector
    case ByteType => PrimitiveObjectInspectorFactory.javaByteObjectInspector
    case NullType => PrimitiveObjectInspectorFactory.javaVoidObjectInspector
  }

  lazy val (objectInspector, instance) = {
    val oi = function.initialize(inspectors.toArray)
    (oi, function)
  }

  def dataType: DataType = inspectorToDataType(objectInspector)

  def wrap(a: Any): Any = a match {
    case s: String => new hadoopIo.Text(s)
    case i: Int => i: java.lang.Integer
    case b: Boolean => b: java.lang.Boolean
    case d: Double => d: java.lang.Double
    case l: Long => l: java.lang.Long
    case l: Short => l: java.lang.Short
    case l: Byte => l: java.lang.Byte
    case s: Seq[_] => seqAsJavaList(s.map(wrap))
    case null => null
  }

  def evaluate(evaluatedChildren: Seq[Any]): Any = {
    val args = evaluatedChildren.map(wrap).map { v =>
      new DeferredJavaObject(v): DeferredObject
    }.toArray
    unwrap(instance.evaluate(args))
  }
}

trait HiveInspectors {
  def toInspectors(exprs: Seq[Expression]) = exprs.map(_.dataType).map {
    case StringType => PrimitiveObjectInspectorFactory.javaStringObjectInspector
    case IntegerType => PrimitiveObjectInspectorFactory.javaIntObjectInspector
    case DoubleType => PrimitiveObjectInspectorFactory.javaDoubleObjectInspector
    case BooleanType => PrimitiveObjectInspectorFactory.javaBooleanObjectInspector
    case LongType => PrimitiveObjectInspectorFactory.javaLongObjectInspector
    case ShortType => PrimitiveObjectInspectorFactory.javaShortObjectInspector
    case ByteType => PrimitiveObjectInspectorFactory.javaByteObjectInspector
  }

  def inspectorToDataType(inspector: ObjectInspector): DataType = inspector match {
    case s: StructObjectInspector =>
      StructType(s.getAllStructFieldRefs.map(f => {
        StructField(f.getFieldName, inspectorToDataType(f.getFieldObjectInspector), true)
      }))
    case l: ListObjectInspector => ArrayType(inspectorToDataType(l.getListElementObjectInspector))
    case m: MapObjectInspector =>
      MapType(
        inspectorToDataType(m.getMapKeyObjectInspector),
        inspectorToDataType(m.getMapValueObjectInspector))
    case _: WritableStringObjectInspector => StringType
    case _: WritableIntObjectInspector => IntegerType
    case _: WritableDoubleObjectInspector => DoubleType
    case _: WritableBooleanObjectInspector => BooleanType
    case _: WritableLongObjectInspector => LongType
    case _: WritableShortObjectInspector => ShortType
    case _: WritableByteObjectInspector => ByteType
  }
}

case class HiveGenericUdaf(
    name: String,
    children: Seq[Expression]) extends AggregateExpression
  with HiveInspectors
  with HiveFunctionFactory {

  type UDFType = AbstractGenericUDAFResolver

  lazy val resolver = createFunction[AbstractGenericUDAFResolver](name)

  lazy val objectInspector: ObjectInspector  = {
    resolver.getEvaluator(children.map(_.dataType.toTypeInfo).toArray)
      .init(GenericUDAFEvaluator.Mode.COMPLETE, inspectors.toArray)
  }

  lazy val inspectors: Seq[ObjectInspector] = toInspectors(children)

  def dataType: DataType = inspectorToDataType(objectInspector)

  def nullable: Boolean = true

  def references: Set[Attribute] = children.map(_.references).flatten.toSet
}
