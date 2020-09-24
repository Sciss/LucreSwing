package de.sciss.lucre.swing

import de.sciss.lucre.expr.Context

import scala.swing.Component

object GridPanelTest extends InMemoryAppLike {
  protected def mkView(): Component = {
    import graph._
    val g = Graph {
      val contents0 = (1 to 3).flatMap { i =>
        val sl      = Slider()
        sl.min      = 1
        sl.max      = 10
        sl.value()  = i * 3
        val lb      = Label(s"Slider $i:")
        lb.hAlign   = Align.Trailing
        lb :: sl :: Nil
      }
      val cb        = CheckBox("Disabled")
      val slE       = Slider()
      slE.enabled   = !cb.selected()
      val flow      = FlowPanel(ComboBox(List(1, 2, 3)))
      flow.align    = Align.Leading
      flow.hGap     = 0
      val contents  = contents0 ++ List(cb, slE, Label("Combo:"), flow)

      val p = GridPanel(contents: _*)
      p.columns = 2
      p.compactColumns = true
      p.border = Border.Empty(4, 8, 4, 8)
      p
    }

    val view = system.step { implicit tx =>
      implicit val ctx: Context[T] = Context()
      g.expand[T]
    }
    view.component
  }
}
