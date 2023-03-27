package at.gepro.dbtemplate

import java.sql._

class SimpleDB(
    connString: String,
    username: String,
    password: String,
  ) {
  private var conn: Connection = _
  private var stmt: Statement = _
  Class.forName("org.postgresql.Driver")
  // establish the database connection and statement on class instantiation
  conn = DriverManager.getConnection(connString, username, password)
  stmt = conn.createStatement()

  def autocommit: Boolean = conn.getAutoCommit()
  def setAutocommit(value: Boolean) = conn.setAutoCommit(value)

  // execute a SQL query that does not return a result set
  def execute(sql: String): Unit = {
    stmt.executeUpdate(sql)
    ()
  }

  def executeQuery(sql: String): List[String] = {
    val rs = stmt.executeQuery(sql)
    if (rs.next()) {
      val metaData = rs.getMetaData()
      val numColumns = metaData.getColumnCount()
      val row = (1 to numColumns).map(i => rs.getString(i)).toList
      row
    }
    else
      throw new RuntimeException(s"No rows found for query: $sql")
  }

  def commit(): Unit = conn.commit()

  def rollback(): Unit = conn.rollback()

  // close the database connection and statement
  def close(): Unit = {
    if (stmt != null)
      stmt.close()
    if (conn != null)
      conn.close()
  }
}
