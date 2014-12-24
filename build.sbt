name := """forms"""

version := "2.0"

lazy val appDependencies = Seq(
  jdbc,
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",
  "com.typesafe.slick" %% "slick" % "3.1.1",
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "javax.mail" % "mail" % "1.4.5",
  "com.unboundid" % "unboundid-ldapsdk" % "2.3.8",
  "org.apache.poi" % "poi-ooxml" % "3.14",
  "org.webjars" % "bootstrap" % "3.3.1",
  "org.webjars" % "font-awesome" % "4.2.0",
  "org.webjars" % "jquery" % "2.1.3",
  "org.webjars" % "handlebars" % "3.0.0-1",
  "org.webjars" % "typeaheadjs" % "0.10.5-1"
)

lazy val codegen = project.settings (
  libraryDependencies ++= Seq(
    "com.typesafe.slick" %% "slick-codegen" % "3.1.1"
  ) ++ appDependencies,
  scalaVersion := "2.11.7"
)

lazy val root = (project in file(".")).dependsOn(codegen).enablePlugins(PlayScala).settings (
  genTables <<= genTablesTask,
  libraryDependencies ++= appDependencies,
  scalaVersion := "2.11.7",
  sourceGenerators in Compile += genTablesTask.taskValue
)

lazy val config = {
  import com.typesafe.config._;
  import java.io.File;
  ConfigFactory.parseFile(new File("conf/application.conf"))
}

lazy val dbURL  = config.getString("slick.dbs.default.db.url")

lazy val dbUser = config.getString("slick.dbs.default.db.user")

lazy val dbPass = config.getString("slick.dbs.default.db.password")

lazy val genTables = TaskKey[Seq[File]]("gen-tables")

lazy val genTablesTask = (sourceManaged, dependencyClasspath in Compile, runner in Compile, streams) map { (dir, cp, r, s) =>
  val outputDir = dir.getPath
  val fname = outputDir + "/db/Tables.scala"
  if (!file(fname).exists) {
    toError(r.run("db.Generator", cp.files, Array(outputDir, dbURL, dbUser, dbPass), s.log))
  }
  Seq(file(fname))
}

sources in (Compile,doc) := Seq.empty

publishArtifact in (Compile, packageDoc) := false
