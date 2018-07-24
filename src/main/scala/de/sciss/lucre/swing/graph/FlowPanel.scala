package de.sciss.lucre.swing
package graph

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.ComponentHolder

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  def mk(configure: FlowPanel => Unit): FlowPanel = {
    val w = apply()
    configure(w)
    w
  }

  private final class Expanded[S <: Sys[S]](contents: Seq[View[S]]) extends View[S]
    with ComponentHolder[scala.swing.FlowPanel] {

    type C = scala.swing.FlowPanel

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val vec = contents.map(_.component)
        component = new scala.swing.FlowPanel(vec: _*)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel {
    override def productPrefix = "FlowPanel" // s"FlowPanel$$Impl" // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      val contentsV = contents.map(_.expand[S])
      new Expanded[S](contentsV).init()
    }
  }
}
trait FlowPanel extends Widget {
  type C = scala.swing.FlowPanel
}
