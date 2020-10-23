package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context

import scala.swing.Component

object GraphTest extends InMemoryAppLike {
  protected def mkView(): Component = {
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

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      g.expand[T]
    }
    view.component
  }
}
