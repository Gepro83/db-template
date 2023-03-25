package at.gepro.dbtemplate

import slick.jdbc.PostgresProfile.api._
import scala.concurrent._

case class Order(id: Int, total: Double)

case class OrderItem(
    orderId: Int,
    name: String,
    price: Double,
  )

sealed trait Action[A] {
  def flatMap[B](f: A => Action[B]): Action[B] = FlatMap(this, f)
  def map[B](f: A => B): Action[B] = flatMap(t => SuccessAction(f(t)))
}

final case class SaveItem(item: OrderItem) extends Action[Unit]
final case class GetOrder(id: Int) extends Action[Order]
final case class SaveOrder(order: Order) extends Action[Unit]
final case class FlatMap[A, B](base: Action[A], f: A => Action[B]) extends Action[B]
final case class SuccessAction[T](value: T) extends Action[T]

trait OrderRepository {
  def saveItem(item: OrderItem): Action[Unit] = SaveItem(item)
  def getOrder(id: Int): Action[Order] = GetOrder(id)
  def saveOrder(order: Order): Action[Unit] = SaveOrder(order)

  def execute[T](action: Action[T]): Future[T]
}

class SlickOrderRepository(
    db: Database,
    items: TableQuery[OrderItemTable],
    orders: TableQuery[OrderTable],
  )(implicit
    ec: ExecutionContext
  ) extends OrderRepository {
  private type SlickAction[T] = DBIOAction[T, NoStream, Effect.All]

  private def toSlickAction[T](action: Action[T]): SlickAction[T] =
    action match {
      case SaveItem(item) => (items += item).map(_ => ())
      case GetOrder(id) => orders.filter(_.id === id).result.map(_.head)
      case SaveOrder(order) => orders.insertOrUpdate(order).map(_ => ())
      case FlatMap(base, f) =>
        // val firstSlickAction: SlickAction[Any] = toSlickAction(base)

        // val combinedSlickAction: SlickAction[T] = firstSlickAction.flatMap { firstResult: Any =>
        //   val secondAction: Action[T] = f(firstResult)
        //   toSlickAction(secondAction)
        // }

        for {
          baseResult <- toSlickAction(base)
          dbAction <- toSlickAction(f(baseResult))
        } yield dbAction
      // combinedSlickAction
      case SuccessAction(value) => DBIO.successful(value)
    }

  override def execute[T](action: Action[T]): Future[T] = {
    val slickAction = toSlickAction(action)

    val transactional = for {
      _ <- sqlu"BEGIN;"
      result <- slickAction
      _ <- sqlu"COMMIT;"
    } yield result

    db.run(transactional)
  }
}

// goal
class OrderService(repo: OrderRepository) {
  def addItem(
      orderId: Int,
      name: String,
      price: Double,
    ): Future[Unit] = {
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
