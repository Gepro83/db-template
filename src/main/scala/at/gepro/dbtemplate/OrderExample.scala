package at.gepro.dbtemplate

case class Order(id: Int, total: Double)
case class OrderItem(
    orderId: Int,
    name: String,
    price: Double,
  )

sealed trait Action[A] {
  def flatMap[B](f: A => Action[B]): Action[B] = FlatMap(this, f)
  def map[B](f: A => B): Action[B] = flatMap(t => SuccessAction(f(t)))
  def transactionally: Action[A] = Transaction(this)
}

final case class SaveItem(item: OrderItem) extends Action[Unit]
final case class GetOrder(id: Int) extends Action[Order]
final case class SaveOrder(order: Order) extends Action[Unit]
final case class FlatMap[A, B](base: Action[A], f: A => Action[B]) extends Action[B]
final case class SuccessAction[T](value: T) extends Action[T]
final case class Transaction[T](action: Action[T]) extends Action[T]
trait OrderRepository {
  def saveItem(item: OrderItem): Action[Unit] = SaveItem(item)
  def getOrder(id: Int): Action[Order] = GetOrder(id)
  def saveOrder(order: Order): Action[Unit] = SaveOrder(order)

  def execute[T](action: Action[T]): T
}

class PostgresOrderRepository(db: SimpleDB) extends OrderRepository {
  private def executeNow[T](action: Action[T]): T =
    action match {
      case SaveItem(item) =>
        db.execute(
          s"INSERT INTO items VALUES(${item.orderId}, '${item.name}', ${item.price});"
        )
      case GetOrder(id) =>
        val row = db.executeQuery(s"SELECT * FROM orders WHERE id = $id;")
        Order(row(0).toInt, row(1).toDouble)

      case SaveOrder(order) =>
        db.execute(s"DELETE FROM orders WHERE id = ${order.id};")
        db.execute(s"INSERT INTO orders VALUES(${order.id}, ${order.total});")

      case FlatMap(base, f) =>
        val baseResult = executeNow(base)
        executeNow(f(baseResult))

      case SuccessAction(value) => value

      case Transaction(action) =>
        action match {
          case a: FlatMap[_, _] =>
            val autocommit = db.autocommit
            db.setAutocommit(false)
            try {
              val result = executeNow(a)
              db.commit()
              result
            }
            catch {
              case e: Throwable =>
                db.rollback()
                throw e
            }
            finally
              db.setAutocommit(autocommit)

          case x => executeNow(x)

        }
    }

  override def execute[T](action: Action[T]): T = executeNow(action)
}

// goal
class OrderService(repo: OrderRepository) {
  def addItem(
      orderId: Int,
      name: String,
      price: Double,
    ) = {
    val item = OrderItem(orderId, name, price)

    // save new item
    // get current order total
    // save order with updated total

    val action = for {
      _ <- repo.saveItem(item)
      order <- repo.getOrder(orderId)
      _ <- repo.saveOrder(order.copy(total = order.total + item.price))
    } yield ()

    repo.execute(action)
  }
}
