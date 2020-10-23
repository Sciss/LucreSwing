package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context

import scala.swing.Component

object BorderPanelTest extends InMemoryAppLike {
  protected def mkView(): Component = {
    import graph._
    val g = Graph {
      val lbN = Label("North" )
      lbN.hAlign = Align.Center
      val lbE = Bang() // Label("East"  )
      val txt = TextField(10)
      txt.text() = "Center"
      val lbS = Label(txt.text())
      lbS.hAlign = Align.Center
      val bp = BorderPanel(north = lbN, east = lbE, center = txt, south = lbS)
      bp.vGap = 4
      bp.border = Border.Empty(8)
      bp
    }

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      val v = g.expand[T]
      v.initControl()
      v
    }
    view.component
  }
}
