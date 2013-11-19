package catalyst
package expressions

object Evaluate {
  def apply(e: Expression, input: Seq[Seq[Any]]): Any = {
    def eval(e: Expression) = Evaluate(e, input)

    e match {
      case Add(l, r) => (eval(l), eval(r)) match {
        case (l: Int, r: Int) => l + r
      }
      case Literal(v, _) => v
      case BoundReference(inputTuple, ordinal, _) => input(inputTuple)(ordinal)
      case other => throw new NotImplementedError(s"Evaluation for:\n $e")
    }
  }
}