package catalyst
package analysis

import expressions._
import plans.logical._
import rules._

/**
 * A trivial [[Analyzer]] with an [[EmptyCatalog]]. Used for testing when all relations are
 * already filled in and the analyser needs only to resolve attribute references.
 *
 */
object SimpleAnalyzer extends Analyzer(EmptyCatalog, EmptyRegistry, true)

class Analyzer(catalog: Catalog, registry: FunctionRegistry, caseSensitive: Boolean)
    extends RuleExecutor[LogicalPlan] {
  val fixedPoint = FixedPoint(100)

  val batches = Seq(
    Batch("LocalRelations", Once,
      NewLocalRelationInstances),
    Batch("CaseInsensitiveAttributeReferences", Once,
      (if(caseSensitive) Nil else LowercaseAttributeReferences :: Nil):_*),
    Batch("Resolution", fixedPoint,
      ResolveReferences,
      ResolveRelations,
      StarExpansion,
      ResolveFunctions),
    Batch("Aggregation", Once,
      GlobalAggregates),
    Batch("Type Coersion", fixedPoint,
      PromoteTypes,
      ConvertNaNs)
  )

  /**
   * Replaces [[UnresolvedRelation]]s with concrete relations from the catalog.
   */
  object ResolveRelations extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case UnresolvedRelation(name, alias) => catalog.lookupRelation(name, alias)
    }
  }

  /**
   * Makes attribute naming case insensitive by turning all UnresolvedAttributes to lowercase.
   */
  object LowercaseAttributeReferences extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case UnresolvedRelation(name, alias) => UnresolvedRelation(name, alias.map(_.toLowerCase))
      case Subquery(alias, child) => Subquery(alias.toLowerCase, child)
      case q: LogicalPlan => q transformExpressions {
        case Star(name) => Star(name.map(_.toLowerCase))
        case UnresolvedAttribute(name) => UnresolvedAttribute(name.toLowerCase)
        case Alias(c, name) => Alias(c, name.toLowerCase)()
      }
    }
  }

  /**
   * Replaces [[UnresolvedAttribute]]s with concrete [[AttributeReference]]s from a logical plan node's children.
   */
  object ResolveReferences extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case q: LogicalPlan if childIsFullyResolved(q) =>
        logger.trace(s"Attempting to resolve ${q.simpleString}")
        q transformExpressions {
        case u @ UnresolvedAttribute(name) =>
          // Leave unchanged if resolution fails.  Hopefully will be resolved next round.
          val result = q.resolve(name).getOrElse(u)
          logger.debug(s"Resolving $u to $result")
          result
        }
    }
  }

  object ResolveFunctions extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case q: LogicalPlan =>
        q transformExpressions {
          case UnresolvedFunction(name, children) if children.map(_.resolved).reduceLeft(_&&_) =>
            registry.lookupFunction(name, children)
        }
    }
  }

  /**
   * Turns projections that contain aggregate expressions into aggregations.
   */
  object GlobalAggregates extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case Project(projectList, child) if containsAggregates(projectList) =>
        Aggregate(Nil, projectList, child)
    }

    def containsAggregates(exprs: Seq[Expression]): Boolean = {
      exprs.foreach(_.foreach {
        case agg: AggregateExpression => return true
        case _ =>
      })
      return false
    }
  }

  /**
   * Expands any references to [[Star]] (*) in project operators.
   */
  object StarExpansion extends Rule[LogicalPlan] {
    def apply(plan: LogicalPlan): LogicalPlan = plan transform {
      case p @ Project(projectList, child) if childIsFullyResolved(p) && containsStar(projectList) =>
        Project(
          projectList.flatMap {
            case Star(None) => child.output
            case Star(Some(table)) => child.output.filter(_.qualifiers contains table)
            case o => o :: Nil
          },
          child)
    }

    /**
     * Returns true if [[exprs]] contains a star.
     */
    protected def containsStar(exprs: Seq[NamedExpression]): Boolean =
      exprs.collect { case Star(_) => true }.nonEmpty
  }

  /**
   * Returns true if all the inputs to the given LogicalPlan node are resolved and non-empty.
   */
  protected def childIsFullyResolved(plan: LogicalPlan): Boolean =
    (!plan.inputSet.isEmpty) && plan.inputSet.map(_.resolved).reduceLeft(_ && _)
}