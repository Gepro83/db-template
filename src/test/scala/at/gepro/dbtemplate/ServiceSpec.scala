package at.gepro.dbtemplate

import scala.collection.mutable

class ServiceSpec extends TestSuite {
  class InMemoryOrderRepository() extends OrderRepository {
    val items: mutable.Map[Int, OrderItem] = mutable.Map()
    val orders: mutable.Map[Int, Order] = mutable.Map()

    override def execute[T](action: Action[T]): T =
      action match {
        case SuccessAction(value) => value
        case GetOrder(id) => orders(id)
        case SaveItem(item) => items(item.orderId) = item
        case SaveOrder(order) => orders(order.id) = order
        case FlatMap(base, f) =>
          val firstResult = execute(base)
          execute(f(firstResult))
      }
  }

  private val repo = new InMemoryOrderRepository()
  private val service = new OrderService(repo)

  test("addItem updates order total") {
    repo.orders(1) = Order(1, 0.0)

    service.addItem(1, "dog-food", 2.0)

    repo.items(1) mustBe OrderItem(1, "dog-food", 2.0)
    repo.orders(1) mustBe Order(1, 2.0)
  }
}
