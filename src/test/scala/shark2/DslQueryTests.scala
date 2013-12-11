package catalyst
package shark2

import org.scalatest.{BeforeAndAfterAll, FunSuite}

import analysis._
import expressions._
import plans._
import plans.logical.LogicalPlan
import types._

/* Implicits */
import dsl._

object TestData {
  import TestShark._

  val testData =
    logical.LocalRelation('key.int, 'value.string)
      .loadData((1 to 100).map(i => (i, i.toString)))

  val testData2 =
    logical.LocalRelation('a.int, 'b.int).loadData(
      (1, 1) ::
        (1, 2) ::
        (2, 1) ::
        (2, 2) ::
        (3, 1) ::
        (3, 2) :: Nil
    )

  val testData3 =
    logical.LocalRelation('a.int, 'b.int).loadData(
      (1, null) ::
        (2, 2) :: Nil
    )

  val upperCaseData =
    logical.LocalRelation('N.int, 'L.string).loadData(
      (null, "") ::
        (1, "A") ::
        (2, "B") ::
        (3, "C") ::
        (4, "D") ::
        (5, "E") ::
        (6, "F") :: Nil
    )

  val lowerCaseData =
    logical.LocalRelation('n.int, 'l.string).loadData(
      (null, "") ::
        (1, "a") ::
        (2, "b") ::
        (3, "c") ::
        (4, "d") :: Nil
    )
}

class DslQueryTests extends FunSuite with BeforeAndAfterAll {
  override def beforeAll() {
    // By clearing the port we force Spark to pick a new one.  This allows us to rerun tests
    // without restarting the JVM.
    System.clearProperty("spark.driver.port")
    System.clearProperty("spark.hostPort")
  }

  import TestShark._
  import TestData._

  test("table scan") {
    checkAnswer(
      testData,
      testData.data)
  }

  test("select *") {
    checkAnswer(
      testData.select(Star(None)),
      testData.data)
  }

  test("simple select") {
    checkAnswer(
      testData.where('key === 1).select('value),
      Seq(Seq("1")))
  }

  test("random sample") {
    testData.where(Rand > 0.5).orderBy(Rand.asc).toRdd.collect()
  }

  test("sorting") {
    checkAnswer(
      testData2.orderBy('a.asc, 'b.asc),
      Seq((1,1), (1,2), (2,1), (2,2), (3,1), (3,2)))

    checkAnswer(
      testData2.orderBy('a.asc, 'b.desc),
      Seq((1,2), (1,1), (2,2), (2,1), (3,2), (3,1)))

    checkAnswer(
      testData2.orderBy('a.desc, 'b.desc),
      Seq((3,2), (3,1), (2,2), (2,1), (1,2), (1,1)))

    checkAnswer(
      testData2.orderBy('a.desc, 'b.asc),
      Seq((3,1), (3,2), (2,1), (2,2), (1,1), (1,2)))
  }

  test("average") {
    checkAnswer(
      testData2.groupBy()(Average('a)),
      2.0)
  }

  test("count") {
    checkAnswer(
      testData2.groupBy()(Count(1)),
      testData2.data.size
    )
  }

  test("null count") {
    checkAnswer(
      testData3.groupBy('a)('a, Count('b)),
      Seq((1,0), (2, 1))
    )

    checkAnswer(
      testData3.groupBy()(Count('a), Count('b), Count(1), CountDistinct('a :: Nil), CountDistinct('b :: Nil)),
      (2, 1, 2, 2, 1) :: Nil
    )
  }

  test("inner join where, one match per row") {
    checkAnswer(
      upperCaseData.join(lowerCaseData, Inner).where('n === 'N),
      Seq(
        (1, "A", 1, "a"),
        (2, "B", 2, "b"),
        (3, "C", 3, "c"),
        (4, "D", 4, "d")
      ))
  }

  test("inner join ON, one match per row") {
    checkAnswer(
      upperCaseData.join(lowerCaseData, Inner, Some('n === 'N)),
      Seq(
        (1, "A", 1, "a"),
        (2, "B", 2, "b"),
        (3, "C", 3, "c"),
        (4, "D", 4, "d")
      ))
  }

  test("inner join, where, multiple matches") {
    val x = testData2.where('a === 1).subquery('x)
    val y = testData2.where('a === 1).subquery('y)
    checkAnswer(
      x.join(y).where("x.a" === "y.a"),
      (1,1,1,1) ::
      (1,1,1,2) ::
      (1,2,1,1) ::
      (1,2,1,2) :: Nil
    )
  }

  test("inner join, no matches") {
    val x = testData2.where('a === 1).subquery('x)
    val y = testData2.where('a === 2).subquery('y)
    checkAnswer(
      x.join(y).where("x.a" === "y.a"),
      Nil)
  }

  test("big inner join, 4 matches per row") {
    val bigData = testData.unionAll(testData).unionAll(testData).unionAll(testData)
    val bigDataX = bigData.subquery('x)
    val bigDataY = bigData.subquery('y)

    checkAnswer(
      bigDataX.join(bigDataY).where("x.key" === "y.key"),
      testData.data.flatMap(row => Seq.fill(16)((row.productIterator ++ row.productIterator).toSeq)))
  }

  test("cartisian product join") {
    checkAnswer(
      testData3.join(testData3),
        (1, null, 1, null) ::
        (1, null, 2, 2) ::
        (2, 2, 1, null) ::
        (2, 2, 2, 2) :: Nil)
  }

  /**
   * Runs the plan and makes sure the answer matches the expected result.
   * @param plan the query to be executed
   * @param expectedAnswer the expected result, can either be an Any, Seq[Product], or Seq[ Seq[Any] ].
   */
  protected def checkAnswer(plan: LogicalPlan, expectedAnswer: Any): Unit = {
    val convertedAnswer = expectedAnswer match {
      case s: Seq[_] if s.isEmpty => s
      case s: Seq[_] if s.head.isInstanceOf[Product] &&
                        !s.head.isInstanceOf[Seq[_]] => s.map(_.asInstanceOf[Product].productIterator.toIndexedSeq)
      case s: Seq[_] => s
      case singleItem => Seq(Seq(singleItem))
    }

    val isSorted = plan.collect { case s: logical.Sort => s}.nonEmpty
    def prepareAnswer(answer: Seq[Any]) = if(!isSorted) answer.sortBy(_.toString) else answer
    val sharkAnswer = plan.toRdd.collect().toSeq
    assert(prepareAnswer(convertedAnswer) === prepareAnswer(sharkAnswer))
  }
}