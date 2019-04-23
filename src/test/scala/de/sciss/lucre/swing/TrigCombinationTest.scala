package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Ex, ExOps}
import de.sciss.lucre.stm.{InMemory, Workspace}

import scala.swing.Component

/*

  The correct behaviour here is that combining the
  two bangs t1 and t2 results in the trigger to be
  passed along in the same event cycle, making it
  therefore possible to combine with the logical
  AND operator, and we see the third bang t3
  illuminated if we click t1.

 */
object TrigCombinationTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val t1 = Bang()
      val t2 = Bang()
      val t3 = Bang()
      t1 ---> t2
      (t1 & t2) ---> t3
      FlowPanel(t1, t2, t3)
    }

    type              S = InMemory
    implicit val sys: S = InMemory()
    import Workspace.Implicits._

    val view = sys.step { implicit tx =>
      implicit val ctx: Ex.Context[S] = Ex.Context()
      val v = g.expand[S]
      v.initControl()
      v
    }
    view.component
  }
}
