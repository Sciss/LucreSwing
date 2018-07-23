package de.sciss.lucre.swing
package graph

import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.impl.ComponentHolder

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  def mk(configure: FlowPanel => Unit): FlowPanel = {
    val w = apply()
    configure(w)
    w
  }

  private final class Expanded[S <: Sys[S]](contents: Seq[View[S]]) extends View[S]
    with ComponentHolder[scala.swing.Label] {

    private[this] var obs: Disposable[S#Tx] = _

    def init()(implicit tx: S#Tx): this.type = {
      ???
      this
    }

    def dispose()(implicit tx: S#Tx): Unit =
      obs.dispose()
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel {
    override def productPrefix: String = s"FlowPanel$$Impl"

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View[S] = {
      val contentsV = contents.map(_.expand[S])
      new Expanded[S](contentsV).init()
    }
  }
}
trait FlowPanel extends Widget {

}
