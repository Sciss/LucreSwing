package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.InMemory

import scala.swing.Component

object GridPanelTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val contents = (1 to 3).flatMap { i =>
        val sl = Slider()
        sl.min    = 1
        sl.max    = 10
        sl.value  = i * 3
        val lb    = Label(s"Slider $i:")
        lb :: sl :: Nil
      }

      val p = GridPanel(contents: _*)
//      p.rows    = 3
      p.columns = 2
      p
    }

    type              S = InMemory
    implicit val sys: S = InMemory()

    val view = sys.step { implicit tx =>
      g.expand[S]
    }
    view.component
  }
}
