package db

import models._

import org.joda.time.DateTime
import slick.driver.MySQLDriver.api._

object TypeMappers {
  implicit val jodaToSql = MappedColumnType.base[DateTime, java.sql.Timestamp](
    { dt => new java.sql.Timestamp(dt.getMillis()) },
    { ts => new DateTime(ts.getTime) }
  )

  implicit val sectionToSection = MappedColumnType.base[SectionKind, String](
    { s => s.name },
    { s => SectionKind.fromName(s) }
  )

  implicit val targetToTarget = MappedColumnType.base[NotificationTarget, String](
    { s => s.name },
    { s => NotificationTarget.fromName(s) }
  )

  implicit val eventToEvent = MappedColumnType.base[Event, String](
    { e => e.name },
    { s => Event.fromName(s) }
  )

  implicit val accessModeToAccessMode = MappedColumnType.base[AccessMode, String](
    { a => a.name },
    { s => AccessMode.fromName(s) }
  )
}
