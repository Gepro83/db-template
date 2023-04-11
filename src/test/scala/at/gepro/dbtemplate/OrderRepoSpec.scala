package at.gepro.dbtemplate

import slick.jdbc.PostgresProfile.api._
import scala.concurrent._
import scala.concurrent.duration._
import SlickMonad._

final class OrderRepoSpec extends IntegrationSpec {
  private val items = TableQuery[OrderItemTable]
  private val orders = TableQuery[OrderTable]
  private val repo = new PostgresOrderRepository(simpleDb)
  // private val repo = new FreeSlickOrderRepository(db, items, orders)
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

    repo.execute(action)

    getItems() mustBe List(catFood)
  }

  test("executing getOrder action returns order") {
    saveOrder(Order(1, 0.0))

    val action = repo.getOrder(1)

    repo.execute(action) mustBe Order(1, 0.0)
  }

  test("executing saveOrder action updates it") {
    saveOrder(Order(1, 0.0))

    val action = repo.saveOrder(Order(1, 1.0))

    repo.execute(action)

    getOrder(1) mustBe Order(1, 1.0)
  }

  test("flatMap results in action sequence") {
    saveOrder(Order(1, 0.0))

    val action: Action[Unit] =
      repo.getOrder(1).flatMap(order => repo.saveOrder(order.copy(total = 1.0)))

    repo.execute(action)

    getOrder(1) mustBe Order(1, 1.0)
  }

  test("map results in mapped action") {
    saveOrder(Order(1, 0.0))

    val action: Action[Double] = repo.getOrder(1).map(_.total)

    repo.execute(action) mustBe 0.0
  }

  test("action is executed transactionally") {
    saveOrder(Order(1, 0.0))

    val action =
      repo.saveOrder(Order(1, 1.0)).flatMap(_ => throw new RuntimeException()).transactionally

    intercept[Exception] {
      repo.execute(action)
    }

    getOrder(1) mustBe Order(1, 0.0)
  }

  test("mark specific action as transactional") {
    val saveOrders = for {
      _ <- repo.saveOrder(Order(1, 1.0))
      _ <- repo.saveOrder(Order(2, 1.0))
    } yield ()

    val saveOrderFailing = repo
      .getOrder(1)
      .flatMap(order => repo.saveOrder(order.copy(total = 2.0)))
      .flatMap(_ => throw new RuntimeException())
      .transactionally

    val combined = for {
      _ <- saveOrders
      _ <- saveOrderFailing
    } yield ()

    intercept[Exception] {
      repo.execute(combined)
    }

    getOrder(1) mustBe Order(1, 1.0)
    getOrder(2) mustBe Order(2, 1.0)
  }
}
