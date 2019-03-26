package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.{InMemory, Workspace}

import scala.swing.Component

object BorderPanelTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val lbN = Label("North" )
      lbN.hAlign = Align.Center
      val lbE = Label("East"  )
      val txt = TextField(10)
      txt.text() = "Center"
      val lbS = Label(txt.text())
      lbS.hAlign = Align.Center
      val bp = BorderPanel(north = lbN, east = lbE, center = txt, south = lbS)
      bp.vGap = 4
      bp.border = Border.Empty(8)
      bp
    }

    type              S = InMemory
    implicit val sys: S = InMemory()
    import Workspace.Implicits._

    val view = sys.step { implicit tx =>
      g.expand[S]()
    }
    view.component
  }
}
