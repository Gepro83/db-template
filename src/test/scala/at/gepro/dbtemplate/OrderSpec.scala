package at.gepro.dbtemplate

import slick.jdbc.PostgresProfile.api._
import scala.concurrent._
import scala.concurrent.duration._

final class OrderSpec extends IntegrationSpec {
  private val items = TableQuery[OrderItemTable]
  private val orders = TableQuery[OrderTable]
  private val repo = new SlickOrderRepository(db, items, orders)
  // private val repo = new alternative.FreeSlickOrderRepository(db, items, orders)
  // import repo._

  private def getItems(): List[OrderItem] =
    Await.result(db.run(items.result.map(_.toList)), 5.seconds)

  private def saveOrder(order: Order) =
    Await.result(db.run(orders += order), 5.seconds)

  private def getOrder(id: Int) =
    Await.result(db.run(orders.filter(_.id === id).result).map(_.head), 5.seconds)

  private val catFood = OrderItem(1, "cat-food", 3.0)

  test("saveItem without execute does nothing") {
    repo.saveItem(catFood)

    getItems() mustBe List()
  }

  test("executing saveItem action saves it") {
    val action = repo.saveItem(catFood)

    whenReady(repo.execute(action)) { _ =>
      getItems() mustBe List(catFood)
    }
  }

  test("executing getOrder action returns order") {
    saveOrder(Order(1, 0.0))

    val action = repo.getOrder(1)

    whenReady(repo.execute(action))(_ mustBe Order(1, 0.0))
  }

  test("executing updateOrder action updates it") {
    saveOrder(Order(1, 0.0))

    val action = repo.saveOrder(Order(1, 1.0))

    whenReady(repo.execute(action)) { _ =>
      getOrder(1) mustBe Order(1, 1.0)
    }
  }

  test("flatMap results in action sequence") {
    saveOrder(Order(1, 0.0))

    val action: Action[Unit] =
      repo.getOrder(1).flatMap(order => repo.saveOrder(order.copy(total = 1.0)))

    whenReady(repo.execute(action)) { _ =>
      getOrder(1) mustBe Order(1, 1.0)
    }
  }

  test("map results in mapped action") {
    saveOrder(Order(1, 0.0))

    val action: Action[Double] = repo.getOrder(1).map(_.total)

    whenReady(repo.execute(action))(_ mustBe 0.0)
  }

  test("action is executed transactionally") {
    saveOrder(Order(1, 0.0))

    val action = repo.saveOrder(Order(1, 1.0)).flatMap(_ => throw new RuntimeException())

    intercept[Exception] {
      Await.result(repo.execute(action), 5.seconds)
    }

    getOrder(1) mustBe Order(1, 0.0)
  }
}
