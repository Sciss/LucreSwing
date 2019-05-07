package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Context, ExOps}
import de.sciss.lucre.stm.{InMemory, Workspace}

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
    import Workspace.Implicits._

    val view = sys.step { implicit tx =>
      implicit val ctx: Context[S] = Context()
      g.expand[S]
    }
    view.component
  }
}
