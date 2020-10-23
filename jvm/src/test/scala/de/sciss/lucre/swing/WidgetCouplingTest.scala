package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context

import scala.swing.Component

object WidgetCouplingTest extends InMemoryAppLike {
  protected def mkView(): Component = {
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

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      g.expand[T]
    }
    view.component
  }
}
