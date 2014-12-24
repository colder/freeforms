package models

import com.pdflib._

object PDFHelpers {

  abstract class Opt {
    def toOptString: String
    def toInnerOptString: String = toOptString
  }

  abstract class OListOpt extends Opt {
    def k: String

    def +(o: OListOpt) = OList(Set(this)) + o
  }

  case class OKeyValue(k: String, v: Opt) extends OListOpt {
    def toOptString = k+"="+v.toInnerOptString

    override def toInnerOptString = "{"+toOptString+"}"
  }
  case class OKey(k: String) extends OListOpt {
    def :=(v: String) = OKeyValue(k, OString(v))
    def :=(v: Opt)    = OKeyValue(k, v)
    def :=(v: Int)    = OKeyValue(k, OString(v.toString))

    def toOptString = k
  }

  implicit def strToOKey(str: String): OKey = OKey(str)

  case class OList(m: Set[OListOpt]) extends Opt {
    def +(o: OListOpt) = {
      OList(m.filter(_.k != o.k) + o)
    }

    def toOptString = {
      m.map(_.toOptString).mkString(" ")
    }

    override def toInnerOptString = "{"+toOptString+"}"
  }

  case object NoOpt extends Opt {
    def toOptString = ""
  }

  case class OString(s: String) extends Opt {
    def toOptString = s
  }

  case class OColor(r: Double, g: Double, b: Double) extends Opt {
    def toOptString = f"{rgb $r%.2f $g%.2f $b%.2f}" 
  }

  object OColor {
    def apply(s: String): OColor = {
      val hex = if (s.length == 7) {
        s.substring(1,7)
      } else {
        s
      }
      val color = Integer.parseInt(hex, 16)

      val r = color/(256*256)
      val g = color/256 % 256
      val b = color % 256

      OColor(r/255d, g/255d, b/255d)
    }
  }

  class PDFAPI(val p: pdflib) {
    // dimentions
    val xleft      = 50d
    val xright     = 545d
    val ybottom    = 792d
    val ytop       = 50d

    // current
    var y     = ytop
    var pages = 0

    def text(str: String, opt: Opt) = {
      PDFText(this, p.create_textflow(str, opt.toOptString))
    }

    def table() = new PDFTable(this)

    def pageBreak(limit: Double) {
      if (y > limit) {
        newPage()
      }
    }

    def newPage() {
      if (pages > 0) {
        p.suspend_page("")
      }
      p.begin_page_ext(0, 0, "width=a4.width height=a4.height")
      pages += 1
      onNewPage(pages)
      y = ytop+25
    }

    def onNewPage(n: Int) {

    }

    def onEnd() {

    }

    def end() {
      p.suspend_page("")

      onEnd()

      p.end_document("")
    }
  }

  class PDFTable(a: PDFAPI) {
    import a._

    var tbl = -1

    var cell = 1;
    var row  = 1;

    def addCell(t: PDFText, opts: Opt) = {
      tbl = p.add_table_cell(tbl, cell, row, "", "textflow="+t.tf+" "+opts.toOptString)
      cell +=1
    }

    def addRow() = {
      cell = 1
      row += 1
    }

    def fitInFlow() = {
      var r = p.fit_table(tbl, xleft, ybottom, xright, y, "rowheightdefault=1 fitmethod=auto")

      if (r == "_error") {
        // Might be that we have not enough space to place one row
        r = "_boxfull"
      }

      while(r == "_boxfull") {
        newPage()
        r = p.fit_table(tbl, xleft, ybottom, xright, y, "rowheightdefault=1")
      }

      y += p.info_table(tbl, "height")+20;

      p.delete_table(tbl, "")
    }

    def fity(ytop: Double, ybot: Double = ybottom) = {
      p.fit_table(tbl, xleft, ybot, xright, ytop, "rowheightdefault=1")
    }
  }

  case class PDFText(a: PDFAPI, tf: Int) {
    import a._

    def fitInFlow() = {
      var r = p.fit_textflow(tf, xleft, ybottom, xright, y, "verticalalign=top")
      while(r == "_boxfull") {
        newPage()
        r = p.fit_textflow(tf, xleft, ybottom, xright, y, "verticalalign=top")
      }
      y += p.info_textflow(tf, "textheight")+10
    }

    def fity(ytop: Double, ybottom: Double = a.ybottom, xleft: Double = a.xleft, xright: Double = a.xright) = {
      p.fit_textflow(tf, xleft, ybottom, xright, ytop, "verticalalign=top")
    }
  }
}
