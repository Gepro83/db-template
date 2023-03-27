package at.gepro.dbtemplate

import scala.collection.mutable

class ServiceSuite extends TestSuite {
  class InMemoryOrderRepository() extends OrderRepository {
    val items: mutable.Map[Int, OrderItem] = mutable.Map()
    val orders: mutable.Map[Int, Order] = mutable.Map()

    private def executeNow[T](action: Action[T]): T =
      action match {
        case SuccessAction(value) => value
        case GetOrder(id) => orders(id)
        case SaveItem(item) => items(item.orderId) = item
        case SaveOrder(order) => orders(order.id) = order
        case FlatMap(base, f) =>
          val firstResult = executeNow(base)
          executeNow(f(firstResult))
        case Transaction(action) => executeNow(action)
      }

    override def execute[T](action: Action[T]): T = executeNow(action)
  }

  private val fastRepo = new InMemoryOrderRepository()
  private val service = new OrderService(fastRepo)

  test("addItem updates order total") {
    fastRepo.orders(1) = Order(1, 0.0)

    service.addItem(1, "dog-food", 2.0)

    fastRepo.items(1) mustBe OrderItem(1, "dog-food", 2.0)
    fastRepo.orders(1) mustBe Order(1, 2.0)
  }
}
