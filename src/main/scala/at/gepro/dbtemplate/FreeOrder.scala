package at.gepro.dbtemplate

import cats.Monad
import scala.concurrent._
import scala.concurrent.duration._
import slick.jdbc.PostgresProfile.api._
import scala.collection.mutable
import cats.Id

sealed trait ActionA[A] // algebra

final case class SaveItemA(item: OrderItem) extends ActionA[Unit]
final case class GetOrderA(id: Int) extends ActionA[Order]
final case class SaveOrderA(order: Order) extends ActionA[Unit]

import cats.free.Free
import cats.free.Free.liftF
import cats.~>

trait FreeOrderRepository {
  type Action[A] = Free[ActionA, A]

  protected type DbAction[_]
  protected def compiler: ActionA ~> DbAction
  protected def run[T](f: DbAction[T]): T

  def saveItem(item: OrderItem): Action[Unit] = liftF(SaveItemA(item))
  def getOrder(id: Int): Action[Order] = liftF(GetOrderA(id))
  def saveOrder(order: Order): Action[Unit] = liftF(SaveOrderA(order))

  def execute[T](action: Action[T])(implicit m: Monad[DbAction]): T = run(
    action.foldMap(compiler)
  )
}

class FreeSlickOrderRepository(
    db: Database,
    items: TableQuery[OrderItemTable],
    orders: TableQuery[OrderTable],
  )(implicit
    ec: ExecutionContext
  ) extends FreeOrderRepository {
  override protected def run[T](action: DbAction[T]): T =
    Await.result(db.run(action.transactionally), 5.seconds)

  override protected type DbAction[T] = DBIOAction[T, NoStream, Effect.All]

  override protected def compiler: ActionA ~> DbAction = new (ActionA ~> DbAction) {
    override def apply[A](action: ActionA[A]): DbAction[A] =
      action match {
        case SaveItemA(item) => (items += item).map(_ => ())
        case GetOrderA(id) => orders.filter(_.id === id).result.map(_.head)
        case SaveOrderA(order) => orders.insertOrUpdate(order).map(_ => ())
      }
  }
}

class FreeInMemoryRepository() extends FreeOrderRepository {
  val items: mutable.Set[OrderItem] = mutable.Set()
  val orders: mutable.Map[Int, Order] = mutable.Map()

  override protected type DbAction[T] = Id[T]
  override protected def compiler: ActionA ~> DbAction = new (ActionA ~> DbAction) {
    override def apply[A](action: ActionA[A]): DbAction[A] =
      action match {
        case GetOrderA(id) => orders(id)
        case SaveItemA(item) => items.add(item); ()
        case SaveOrderA(order) => orders(order.id) = order
      }
  }

  override protected def run[T](a: DbAction[T]): T = a
}
