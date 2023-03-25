package at.gepro.dbtemplate

import slick.jdbc.PostgresProfile.api._

class OrderTable(tag: Tag) extends Table[Order](tag, "orders") {
  def id = column[Int]("id", O.PrimaryKey)
  def total = column[Double]("total")
  override def * = (id, total).<>(Order.tupled, Order.unapply _)
}

class OrderItemTable(tag: Tag) extends Table[OrderItem](tag, "items") {
  def orderId = column[Int]("orderid")
  def name = column[String]("name")
  def price = column[Double]("price")

  def pk = primaryKey("pk_a", (orderId, name, price))

  override def * = (orderId, name, price).<>(OrderItem.tupled, OrderItem.unapply _)
}
