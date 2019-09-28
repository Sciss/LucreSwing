package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context
import de.sciss.lucre.stm.{InMemory, UndoManager, Workspace}

import scala.swing.Component

object PathFieldFolderTest extends AppLike {
  protected def mkView(): Component = {
    import graph._
    val g = Graph {
      val lb = Label("Choose Folder:")
      val gg = PathField()
      gg.mode = PathField.Folder
      FlowPanel(lb, gg)
    }

    type              S = InMemory
    implicit val sys: S = InMemory()
    implicit val undo: UndoManager[S] = UndoManager()
    import Workspace.Implicits._

    val view = sys.step { implicit tx =>
      implicit val ctx: Context[S] = Context()
      g.expand[S]
    }
    view.component
  }
}