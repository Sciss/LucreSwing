package de.sciss.lucre.swing

import java.io.File

import de.sciss.lucre.expr.Context

import scala.swing.Component

object DropTargetTest extends InMemoryAppLike {
  protected def mkView(): Component = {
    import graph._
    val g = Graph {
      val ggEnabled         = CheckBox("enabled")
      ggEnabled.selected()  = true
      val tgt               = DropTarget()
      tgt.enabled           = ggEnabled.selected()
      val b                 = Bang()
      val drop              = tgt.select[File]
      val ggPath            = Label(drop.value.toStr)
//      val ggPath            = PathField()
//      ggPath.value <--- drop.value
      drop.received ---> b
      BorderPanel(
        north = FlowPanel(ggEnabled, tgt, b),
        south = ggPath
      )
    }

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      g.expand[T]
    }
    view.component
  }
}
