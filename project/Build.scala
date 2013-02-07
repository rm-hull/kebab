import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "kebab"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
      "com.ning" % "async-http-client" % "1.7.10"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      // Add your own project settings here
      scalaVersion := "2.10.0"
    )

}
