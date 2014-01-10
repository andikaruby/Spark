package catalyst
package execution

import catalyst.expressions.{SortOrder, Expression}

trait DataProperty {
  def expressions: Seq[Expression]

  /**
   * If we need an [[catalyst.execution.Exchange]] to re-partition data for
   * the given [[catalyst.execution.DataProperty]] other.
   * @param other The given [[catalyst.execution.DataProperty]].
   *@return
   */
  // TODO: We should also consider functional dependencies between expressions of
  // two data properties. For example, if we have a GroupProperty(a) and a
  // GroupProperty(a+1), we will not need an exchange to re-partition the data.
  def needExchange(other: DataProperty): Boolean
}

/**
 * An implementation of [[catalyst.execution.DataProperty]] represents that
 * the data property of a dataset is not specified.
 * If it is used as a required data property for a physical operator
 * (a [[catalyst.execution.SharkPlan]]) (e.g. [[catalyst.execution.Project]]),
 * it means that this operator does not require its input datasets to be
 * organized in a certain way.
 */
case class NotSpecifiedProperty() extends DataProperty {
  def expressions = Nil

  def needExchange(other: DataProperty): Boolean = {
    other match {
      case NotSpecifiedProperty() => false
      case GroupProperty(groupingExpressions) => true
      case SortProperty(_) => true
    }
  }
}

/**
 * An implementation of [[catalyst.execution.DataProperty]] represents that
 * a dataset is grouped by groupingExpressions.
 * @param groupingExpressions The expressions used to specify the way how rows should be grouped.
 *                            If it is a Nil, the entire dataset is considered as a single group.
 *                            In this case, a single reducer will be used.
 */
case class GroupProperty(groupingExpressions: Seq[Expression]) extends DataProperty {
  override val expressions = groupingExpressions

  def needExchange(other: DataProperty): Boolean = {
    other match {
      case NotSpecifiedProperty() => false
      // We do not need an Exchange operator if another GroupProperty only
      // needs to group rows within a partition.
      case g @ GroupProperty(otherExpressions) => {
        if (expressions.toSet.subsetOf(otherExpressions.toSet)) false else true
      }
      // Because we use [[org.apache.spark.HashPartitioner]] for GroupProperty,
      // we need to use an Exchange operator to sort data with a
      // [[org.apache.spark.RangePartitioner]]. But, if the groupingExpressions is
      // a Nil, we will have a single partition. So, we do not need an Exchange operator
      // to sort this single partition.
      case SortProperty(otherExpressions) => if (expressions == Nil) false else true
    }
  }
}

/**
 * An implementation of [[catalyst.execution.DataProperty]] represents that
 * a dataset is sorted by sortingExpressions. A SortProperty also implies that
 * the dataset is grouped by sortingExpressions.
 * @param sortingExpressions The expressions used to specify the way how rows should be sorted.
 *                           sortingExpressions should not be empty.
 */
case class SortProperty(sortingExpressions: Seq[SortOrder]) extends DataProperty {

  {
    if (sortingExpressions == Nil) {
      throw new IllegalArgumentException("Sorting expressions of a SortProperty " +
        "are not specified.")
    }
  }

  def expressions = sortingExpressions

  def needExchange(other: DataProperty): Boolean = {
    other match {
      case NotSpecifiedProperty() => false
      // A SortProperty implies a GroupProperty. We do not need an Exchange operator
      // if the GroupProperty only needs to group rows within a partition.
      case g @ GroupProperty(otherExpressions) => {
        if (expressions.map(expression => expression.child).
          toSet.subsetOf(otherExpressions.toSet)) false else true
      }
      // We do not need an Exchange operator if another SortProperty only needs to
      // sort rows within a partition (cases satisfying otherExpressions.startsWith(expressions))
      // or we do not need to sort again (cases satisfying
      // expressions.startsWith(otherExpressions)).
      case s @ SortProperty(otherExpressions) => {
        if (otherExpressions.startsWith(expressions) ||
          expressions.startsWith(otherExpressions)) false else true
      }
    }
  }
}