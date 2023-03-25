package at.gepro.dbtemplate.alternative

import scala.concurrent._
import slick.jdbc.PostgresProfile.api._
import scala.collection.mutable
import at.gepro.dbtemplate.OrderItem
import at.gepro.dbtemplate.Order
import at.gepro.dbtemplate.OrderItemTable
import at.gepro.dbtemplate.OrderTable
import cats.instances.future._

sealed trait ActionA[A] // algebra

final case class SaveItemA(item: OrderItem) extends ActionA[Unit]
final case class GetOrderA(id: Int) extends ActionA[Order]
final case class SaveOrderA(order: Order) extends ActionA[Unit]

import cats.free.Free
import cats.free.Free.liftF
import cats.~>

trait FreeOrderRepository {
  type Action[A] = Free[ActionA, A]

  protected def compiler: ActionA ~> Future

  def saveItem(item: OrderItem): Action[Unit] = liftF(SaveItemA(item))
  def getOrder(id: Int): Action[Order] = liftF(GetOrderA(id))
  def saveOrder(order: Order): Action[Unit] = liftF(SaveOrderA(order))

  def execute[T](action: Action[T])(implicit ec: ExecutionContext): Future[T] =
    action.foldMap(compiler)
}

class FreeSlickOrderRepository(
    db: Database,
    items: TableQuery[OrderItemTable],
    orders: TableQuery[OrderTable],
  )(implicit
    ec: ExecutionContext
  ) extends FreeOrderRepository {
  override protected def compiler: ActionA ~> Future = new (ActionA ~> Future) {
    override def apply[A](action: ActionA[A]): Future[A] =
      action match {
        case SaveItemA(item) => db.run((items += item).map(_ => ()))
        case GetOrderA(id) => db.run(orders.filter(_.id === id).result.map(_.head))
        case SaveOrderA(order) => db.run(orders.insertOrUpdate(order).map(_ => ()))
      }
  }
}

// no need to show
class FreeInMemoryRepository() extends FreeOrderRepository {
  val items: mutable.Set[OrderItem] = mutable.Set()
  val orders: mutable.Map[Int, Order] = mutable.Map()

  override protected def compiler: ActionA ~> Future = new (ActionA ~> Future) {
    override def apply[A](action: ActionA[A]): Future[A] =
      action match {
        case GetOrderA(id) => Future.successful(orders(id))
        case SaveItemA(item) => items.add(item); Future.successful(())
        case SaveOrderA(order) => orders(order.id) = order; Future.successful(())
      }
  }
}
