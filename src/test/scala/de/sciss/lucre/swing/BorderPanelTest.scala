package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.InMemory

import scala.swing.Component

object BorderPanelTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val lbN = Label("North" )
      val lbE = Label("East"  )
      val txt = TextField(10)
      txt.text = "Center"
      val lbS = Label(txt.text)
      BorderPanel(north = lbN, east = lbE, center = txt, south = lbS)
    }

    type              S = InMemory
    implicit val sys: S = InMemory()

    val view = sys.step { implicit tx =>
      g.expand[S]
    }
    view.component
  }
}
