package at.gepro.dbtemplate

import slick.jdbc.PostgresProfile.api._
import scala.concurrent._
import scala.concurrent.duration._
import java.util.concurrent.Executors

object IntegrationSpec {
  private val db = Database.forConfig("postgresDb")
  private val simpleDb = new SimpleDB("jdbc:postgresql://localhost:5432/db", "postgres", "postgres")
}

trait IntegrationSpec extends TestSuite {
  implicit val ec: ExecutionContext =
    ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))

  override protected def beforeAll(): Unit = {
    val stmts = """
      | CREATE TABLE IF NOT EXISTS orders (
      |   id INT PRIMARY KEY,
      |   total DOUBLE PRECISION
      | );
      | CREATE TABLE IF NOT EXISTS items (
      |   orderid INT,
      |   name VARCHAR(255),
      |   price DOUBLE PRECISION,
      |   PRIMARY KEY (orderid, name, price)
      |);
      | 
    """.stripMargin

    val result = db.run(sqlu"#$stmts").map(_ => ())
    Await.result(result, 10.seconds)
  }

  override protected def afterEach(): Unit = {
    val result = db.run(sqlu"TRUNCATE orders, items;")
    Await.result(result, 10.seconds)
    super.afterEach()
  }

  override protected def afterAll(): Unit = {
    db.close()
    simpleDb.close()
  }

  def db: Database = IntegrationSpec.db
  def simpleDb: SimpleDB = IntegrationSpec.simpleDb
}
