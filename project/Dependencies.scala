import sbt._

object Dependencies {
  val custom = Seq(
    "com.typesafe.slick" %% "slick" % "3.4.1",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.4.1",
    "org.slf4j" % "slf4j-nop" % "1.6.4", // slick needs that, does no logging, replace with real logging framework
    "org.postgresql" % "postgresql" % "42.6.0",
    "org.typelevel" %% "cats-core" % "2.9.0",
    "org.typelevel" %% "cats-free" % "2.9.0",
  )

  object com {
    object eed3si9n {
      object expecty {
        val expecty =
          "com.eed3si9n.expecty" %% "expecty" % "0.16.0"
      }
    }

    object github {
      object alexarchambault {
        val `scalacheck-shapeless_1.16` =
          "com.github.alexarchambault" %% "scalacheck-shapeless_1.16" % "1.3.1"
      }

      object liancheng {
        val `organize-imports` =
          "com.github.liancheng" %% "organize-imports" % "0.6.0"
      }
    }

    object olegpy {
      val `better-monadic-for` =
        "com.olegpy" %% "better-monadic-for" % "0.3.1"
    }
  }

  object org {
    object augustjune {
      val `context-applied` =
        "org.augustjune" %% "context-applied" % "0.1.4"
    }

    object scalacheck {
      val scalacheck =
        "org.scalacheck" %% "scalacheck" % "1.16.0"
    }

    object scalatest {
      val scalatest =
        "org.scalatest" %% "scalatest" % "3.2.14"
    }

    object scalatestplus {
      val `scalacheck-1-16` =
        "org.scalatestplus" %% "scalacheck-1-16" % "3.2.14.0"
    }

    object typelevel {
      val `discipline-scalatest` =
        "org.typelevel" %% "discipline-scalatest" % "2.2.0"

      val `kind-projector` =
        "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
    }
  }
}
