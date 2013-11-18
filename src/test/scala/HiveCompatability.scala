package catalyst

import shark.SharkContext
import shark.SharkEnv

import java.io._
import org.scalatest.{BeforeAndAfterAll, FunSuite, GivenWhenThen}

import frontend.Hive
import util.TestShark

class HiveCompatability extends FunSuite with BeforeAndAfterAll with GivenWhenThen {
  /** A list of tests currently deemed out of scope and thus completely ignored */
  val blackList = Seq(
    "set_processor_namespaces" // Unclear how we want to handle the
  )

  /** The set of tests that are believed to be working in catalyst. Tests not in white */
  val whiteList = Seq(
    "tablename_with_select"
  )

  override def beforeAll() {
    // By clearing the port we force Spark to pick a new one.  This allows us to rerun tests
    // without restarting the JVM.
    System.clearProperty("spark.driver.port")

    testShark.sc.runSql("CREATE TABLE src (key INT, value STRING)")
    testShark.sc.runSql("""LOAD DATA LOCAL INPATH '/Users/marmbrus/workspace/hive/data/files/kv1.txt' INTO TABLE src""")
  }

  val testShark = new TestShark

  // TODO: bundle in jar files... get from classpath
  val hiveQueryDir = new File("/Users/marmbrus/workspace/hive/ql/src/test/queries/clientpositive")
  val testCases = hiveQueryDir.listFiles

  // Go through all the test cases and add them to scala test.
  testCases.foreach { testCase =>
    val testCaseName = testCase.getName.stripSuffix(".q")
    if(blackList contains testCaseName) {
      // Do nothing
    } else if(whiteList contains testCaseName) {
      // Build a test case and submit it to scala test framework...
      test(testCaseName) {
        val queriesString = fileToString(testCase)
        val queryList = queriesString.split("(?<=[^\\\\]);").map(_.trim).filterNot(_ == "")

        val sharkResults = queryList.map { queryString =>
          info(queryString)
          testShark.sc.runSql(queryString)
        }

        // Run w/ catalyst
        val catalystResults = queryList.map { queryString =>
          info(queryString)
          val query = new testShark.SharkQuery(queryString)
          Option(query.execute())
        }
      }
    } else {
      ignore(testCaseName) {}
    }
  }

  protected def fileToString(file: File, encoding: String = "UTF-8") = {
    val inStream = new FileInputStream(file)
    val outStream = new ByteArrayOutputStream
    try {
      var reading = true
      while ( reading ) {
        inStream.read() match {
          case -1 => reading = false
          case c => outStream.write(c)
        }
      }
      outStream.flush()
    }
    finally {
      inStream.close()
    }
    new String(outStream.toByteArray(), encoding)
  }
}
