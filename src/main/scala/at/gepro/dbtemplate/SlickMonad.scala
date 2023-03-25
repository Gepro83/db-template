package at.gepro.dbtemplate

import cats.Monad
import slick.dbio._
import scala.concurrent.ExecutionContext

object SlickMonad extends SlickMonad

trait SlickMonad {
  implicit def slickMonad(implicit ec: ExecutionContext): Monad[DBIO] =
    new Monad[DBIO] {
      override def flatMap[A, B](fa: DBIO[A])(f: A => DBIO[B]): DBIO[B] = fa.flatMap(f)

      override def tailRecM[A, B](a: A)(f: A => DBIO[Either[A, B]]): DBIO[B] =
        f(a).flatMap {
          case Right(b) => DBIO.successful(b)
          case Left(a1) => tailRecM(a1)(f)
        }

      override def pure[A](x: A): DBIO[A] = DBIO.successful(x)
    }
}
