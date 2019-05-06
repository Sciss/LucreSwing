package de.sciss.lucre.swing

import java.io.File

import de.sciss.lucre.expr.{Ex, ExOps}
import de.sciss.lucre.stm.{InMemory, Workspace}

import scala.swing.Component

object DropTargetTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
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

    type              S = InMemory
    implicit val sys: S = InMemory()
    import Workspace.Implicits._

    val view = sys.step { implicit tx =>
      implicit val ctx: Ex.Context[S] = Ex.Context()
      g.expand[S]
    }
    view.component
  }
}
