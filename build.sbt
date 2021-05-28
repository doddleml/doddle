
lazy val root = (project in file("."))
  .settings(
    name := "doddle-model",
    organization := "io.github.picnicml",
    homepage := Some(url("https://picnicml.github.io")),
    scmInfo := Some(ScmInfo(
      url("https://github.com/picnicml/doddle-model"),
      "https://github.com/picnicml/doddle-model.git")
    ),
    version := Version(),
    crossScalaVersions := Seq("2.13.0", "2.12.14", "2.11.12"),
    scalaVersion := crossScalaVersions.value.head,
    libraryDependencies ++= Dependencies.settings,
    developers := List(
      Developer("inejc", "Nejc Ilenic", "nejc.ilenic@gmail.com", url("https://github.com/inejc"))
    ),
    licenses := List("Apache-2.0" -> url("https://opensource.org/licenses/Apache-2.0")),
    publishMavenStyle := true,
    publishTo := Some(
      if (isSnapshot.value)
        Opts.resolver.sonatypeSnapshots
      else
        Opts.resolver.sonatypeStaging
    ),
    scalacOptions ++= (Seq(
      "-deprecation",
      "-encoding", "UTF-8",
      "-feature",
      "-language:existentials",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-unchecked",
      "-Xfatal-warnings",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard"
    ) ++ (CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, scalaMajor)) if scalaMajor >= 13 => Seq.empty[String]
      case _ => Seq(
        "-Yno-adapted-args",
        "-Xfuture"
      )
    })),
    unmanagedSourceDirectories in Compile += {
      val sourceDir = (sourceDirectory in Compile).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, scalaMajor)) if scalaMajor >= 13 => sourceDir / "scala-2.13+"
        case _ => sourceDir / "scala-2.12-"
      }
    }
  )
