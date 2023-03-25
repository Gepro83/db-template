package at.gepro
package dbtemplate

import com.eed3si9n.expecty._
import org.scalacheck.ScalacheckShapeless
import org.scalatest._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.typelevel.discipline.scalatest.FunSuiteDiscipline
import org.scalatest.concurrent.ScalaFutures

trait TestSuite
    extends AnyFunSuite
       with must.Matchers
       with GivenWhenThen
       with BeforeAndAfterAll
       with BeforeAndAfterEach
       with ScalaCheckPropertyChecks
       with ScalacheckShapeless
       with FunSuiteDiscipline
       with ScalaFutures {
  final protected type Arbitrary[A] =
    org.scalacheck.Arbitrary[A]

  final protected val Arbitrary: org.scalacheck.Arbitrary.type =
    org.scalacheck.Arbitrary

  final protected type Assertion =
    org.scalatest.compatible.Assertion

  final protected type Gen[+A] =
    org.scalacheck.Gen[A]

  final protected val Gen: org.scalacheck.Gen.type =
    org.scalacheck.Gen

  final protected val expect: VarargsExpecty =
    Expecty.expect
}
