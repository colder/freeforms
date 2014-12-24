package db
import slick.driver.MySQLDriver
import slick.codegen.SourceCodeGenerator
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Generator {
  def main(args: Array[String]) = {
    val outDir = args(0)
    val url    = args(1)
    val user   = args(2)
    val pass   = args(3)

    val db = MySQLDriver.api.Database.forURL(url,driver="com.mysql.jdbc.Driver",user=user,password=pass)

    val modelAction = MySQLDriver.createModel(Some(MySQLDriver.defaultTables))
    val modelFuture = db.run(modelAction)

    val codeGenFuture = modelFuture.map(model => new SourceCodeGenerator(model) {
      override def code = "import db.TypeMappers._" + "\n" +
                          "import models._" + "\n" +
                          "import org.joda.time.DateTime" + "\n" +
                          super.code

      // override generator responsible for tables
      override def Table = new Table(_){
        table =>
        override def Column = new Column(_){
          override def rawType = {
            (super.rawType, model.name) match {
              case (_, "year")                     => "Int"
              case ("java.sql.Timestamp", _)       => "DateTime"
              case ("java.sql.Date", _)            => "DateTime"
              case ("String", "section")           => "SectionKind"
              case ("String", "event")             => "Event"
              case ("String", "access_mode")       => "AccessMode"
              case ("String", "target")            => "NotificationTarget"
              case ("String", "optional")          => "Boolean"
              case ("String", "hasNA")             => "Boolean"
              case ("String", "midterm")           => "Boolean"
              case ("String", "freeifselected")    => "Boolean"
              case ("String", "joint_complete")    => "Boolean"
              case ("String", "student_complete")  => "Boolean"
              case ("String", "director_complete") => "Boolean"
              case (t, _ )                         => t
            }
          }
        }
      }
    })

    Await.result(codeGenFuture.map { case codegen =>
      codegen.writeToFile(
            "slick.driver.MySQLDriver",
            outDir,
            "db",
            "Tables",
            "Tables.scala"
          )
    }, Duration.Inf)
  }

}
