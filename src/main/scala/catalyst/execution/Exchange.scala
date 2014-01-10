package catalyst
package execution

import catalyst.rules.Rule
import catalyst.errors._
import catalyst.expressions._
import catalyst.types._

import org.apache.spark.{RangePartitioner, HashPartitioner}
import org.apache.spark.rdd.ShuffledRDD

case class Exchange(
    dataProperty: DataProperty,
    child: SharkPlan,
    numPartitions: Int = 8) extends UnaryNode {

  def output = child.output

  def execute() = attachTree(this , "execute") {
    dataProperty match {
      case NotSpecifiedProperty() => child.execute()
      case g @ GroupProperty(groupingExpressions) => {
        val rdd = child.execute().map { row =>
          (buildRow(groupingExpressions.toSeq.map(Evaluate(_, Vector(row)))), row)
        }
        val part = new HashPartitioner(numPartitions)
        val shuffled = new ShuffledRDD[Row, Row, (Row, Row)](rdd, part)

        shuffled.map(_._2)
      }
      case s @ SortProperty(sortingExpressions) => {
        val directions = sortingExpressions.map(_.direction).toIndexedSeq
        val dataTypes = sortingExpressions.map(_.dataType).toIndexedSeq

        class SortKey(val keyValues: IndexedSeq[Any])
          extends Ordered[SortKey]
          with Serializable {
          def compare(other: SortKey): Int = {
            var i = 0
            while (i < keyValues.size) {
              val left = keyValues(i)
              val right = other.keyValues(i)
              val curDirection = directions(i)
              val curDataType = dataTypes(i)

              logger.debug(s"Comparing $left, $right as $curDataType order $curDirection")
              // TODO: Use numeric here too?
              val comparison =
                if (left == null && right == null) {
                  0
                } else if (left == null) {
                  if (curDirection == Ascending) -1 else 1
                } else if (right == null) {
                  if (curDirection == Ascending) 1 else -1
                } else if (curDataType == IntegerType) {
                  if (curDirection == Ascending) {
                    left.asInstanceOf[Int] compare right.asInstanceOf[Int]
                  } else {
                    right.asInstanceOf[Int] compare left.asInstanceOf[Int]
                  }
                } else if (curDataType == DoubleType) {
                  if (curDirection == Ascending) {
                    left.asInstanceOf[Double] compare right.asInstanceOf[Double]
                  } else {
                    right.asInstanceOf[Double] compare left.asInstanceOf[Double]
                  }
                } else if (curDataType == LongType) {
                  if (curDirection == Ascending) {
                    left.asInstanceOf[Long] compare right.asInstanceOf[Long]
                  } else {
                    right.asInstanceOf[Long] compare left.asInstanceOf[Long]
                  }
                } else if (curDataType == StringType) {
                  if (curDirection == Ascending) {
                    left.asInstanceOf[String] compare right.asInstanceOf[String]
                  } else {
                    right.asInstanceOf[String] compare left.asInstanceOf[String]
                  }
                } else {
                  sys.error(s"Comparison not yet implemented for: $curDataType")
                }

              if (comparison != 0) return comparison
              i += 1
            }
            return 0
          }
        }

        val rdd = child.execute().map { row =>
          val input = Vector(row)
          val sortKey = new SortKey(
            sortingExpressions.map(s => Evaluate(s.child, input)).toIndexedSeq)

          (sortKey, row)
        }
        val part = new RangePartitioner(numPartitions, rdd, ascending = true)
        val shuffled = new ShuffledRDD[SortKey, Row, (SortKey, Row)](rdd, part)

        shuffled.map(_._2)
      }
    }
  }
}

object AddExchange extends Rule[SharkPlan] {
  def apply(plan: SharkPlan): SharkPlan = {
    // TODO: determine the number of partitions.
    // TODO: We need to consider the number of partitions to determine if we
    // will add an Exchange operator. If a dataset only has a single partition,
    // even if needExchange returns true, we do not need to shuffle the data again.
    val numPartitions = 8
    plan.transformUp {
      case aggregate @ Aggregate(
        groupingExpressions,
        aggregateExpressions,
        child) => {
        if (child.outputDataProperty.needExchange(aggregate.requiredDataProperty)) {
          val exchange = new Exchange(aggregate.requiredDataProperty, child, numPartitions)

          Aggregate(groupingExpressions, aggregateExpressions, exchange)()
        } else {
          aggregate
        }
      }
      case equiInnerJoin @ SparkEquiInnerJoin(
        leftKeys,
        rightKeys,
        left,
        right) => {
        val newLeft = {
          if (left.outputDataProperty.needExchange(equiInnerJoin.leftRequiredDataProperty)) {
            new Exchange(equiInnerJoin.leftRequiredDataProperty, left, numPartitions)
          } else {
            left
          }
        }

        val newRight = {
          if (right.outputDataProperty.needExchange(equiInnerJoin.rightRequiredDataProperty)) {
            new Exchange(equiInnerJoin.rightRequiredDataProperty, right, numPartitions)
          } else {
            right
          }
        }

        SparkEquiInnerJoin(leftKeys, rightKeys, newLeft, newRight)()
      }
      case sort @ Sort(sortExprs, child) => {
        if (child.outputDataProperty.needExchange(sort.requiredDataProperty)) {
          val exchange = new Exchange(sort.requiredDataProperty, child, numPartitions)

          Sort(sortExprs, exchange)()
        } else {
          sort
        }
      }
    }
  }
}