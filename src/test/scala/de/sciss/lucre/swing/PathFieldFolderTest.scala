package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context

import scala.swing.Component

object PathFieldFolderTest extends InMemoryAppLike {
  protected def mkView(): Component = {
    import graph._
    val g = Graph {
      val lb = Label("Choose Folder:")
      val gg = PathField()
      gg.mode = PathField.Folder
      FlowPanel(lb, gg)
    }

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      g.expand[T]
    }
    view.component
  }
}