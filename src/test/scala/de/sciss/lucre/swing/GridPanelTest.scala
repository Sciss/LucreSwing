package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.{InMemory, WorkspaceHandle}

import scala.swing.Component

object GridPanelTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val contents0 = (1 to 3).flatMap { i =>
        val sl      = Slider()
        sl.min      = 1
        sl.max      = 10
        sl.value()  = i * 3
        val lb      = Label(s"Slider $i:")
        lb :: sl :: Nil
      }
      val cb        = CheckBox("Disabled")
      val slE       = Slider()
      slE.enabled   = !cb.selected()
      val contents  = contents0 ++ List(cb, slE)

      val p = GridPanel(contents: _*)
      p.columns = 2
      p
    }

    type              S = InMemory
    implicit val sys: S = InMemory()
    import WorkspaceHandle.Implicits._

    val view = sys.step { implicit tx =>
      g.expand[S]()
    }
    view.component
  }
}
