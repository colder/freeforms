package models

abstract class Access(val isReadable: Boolean, val isWriteable: Boolean)
case object Writeable extends Access(true, true)
case object ReadOnly extends Access(true, false)
case object Denied extends Access(false, false)
