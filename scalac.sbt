import Scalac.Keys._

ThisBuild / scalacOptions ++= Seq(
  "-language:_",
  "-Ymacro-annotations",
  "-Wconf:any:silent",
  "-Wunused:imports", // always on for OrganizeImports
) ++ Seq("-encoding", "UTF-8") ++ warnings.value ++ lint.value

ThisBuild / warnings := {
  if (insideCI.value)
    Seq(
      "-Wconf:any:error", // for scalac warnings
      "-Xfatal-warnings", // for wartremover warts
    )
  else if (lintOn.value)
    Seq()
  else
    Seq("-Wconf:any:silent")
}

ThisBuild / lintOn :=
  !sys.env.contains("LINT_OFF")

ThisBuild / lint := {
  if (shouldLint.value)
    Scalac.Lint
  else
    Seq.empty
}

ThisBuild / shouldLint :=
  insideCI.value || lintOn.value
