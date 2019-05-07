package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Context, ExOps}
import de.sciss.lucre.stm.{InMemory, Workspace}

import scala.swing.Component

object WidgetCouplingTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val items = List("alpha", "beta", "gamma")
      val c1    = ComboBox[String](items)
      val c2    = ComboBox[String](items)
      c2.index <--- c1.index
      BorderPanel(
        center = FlowPanel(c1, Label("->"), c2),
        north = Label("TODO: Index is not reactive yet")
      )
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
