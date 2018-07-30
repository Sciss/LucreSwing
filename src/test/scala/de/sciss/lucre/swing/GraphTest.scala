package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.{InMemory, WorkspaceHandle}

import scala.swing.Component

object GraphTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val sl      = Slider()
      sl.min      = 1
      sl.max      = 10
      sl.value()  = 1
      val txt     = sl.value().toStr
      val lb      = Label(txt)
      FlowPanel(sl, Separator(), lb)
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
