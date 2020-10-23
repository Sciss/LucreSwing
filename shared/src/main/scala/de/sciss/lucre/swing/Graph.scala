/*
 *  Graph.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.expr.graph.Control
import de.sciss.lucre.expr.impl.{ExElem, GraphBuilderMixin, GraphFormatMixin}
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.graph.Widget
import de.sciss.lucre.{Txn, expr}
import de.sciss.serial.{ConstFormat, DataInput, DataOutput}

import scala.collection.immutable.{IndexedSeq => Vec, Seq => ISeq}

object Graph {
  type Builder = expr.Graph.Builder

  def apply(thunk: => Widget): Graph = {
    val b = new BuilderImpl
    use(b) {
      val w = thunk
      b.build(w)
    }
  }

  def use[A](b: Builder)(body: => A): A = expr.Graph.use(b)(body)

  def builder: Builder = expr.Graph.builder

  private[this] final class BuilderImpl extends GraphBuilderMixin {
    override def toString = s"lucre.swing.Graph.Builder@${hashCode.toHexString}"

    def build(w: Widget): Graph = {
      val configured = buildControls()
      Graph(w, configured)
    }
  }

  implicit object format extends ConstFormat[Graph] with GraphFormatMixin {
    private final val SER_VERSION = 0x5778  // "Wx"

    def write(g: Graph, out: DataOutput): Unit = {
      out.writeShort(SER_VERSION)
      var ref = null: ExElem.RefMapOut
      ref = ExElem.write(g.widget, out, ref)
      val cx = g.controls
      writeControls(cx, out, ref)
    }

    def read(in: DataInput): Graph = {
      val cookie = in.readShort()
      require(cookie == SER_VERSION, s"Unexpected cookie $cookie")
      val ref = new ExElem.RefMapIn
      val w   = ExElem.read (in, ref).asInstanceOf[Widget]
      val cx  = readControls(in, ref)
      Graph(w, cx)
    }
  }

  private final class ExpandedImpl[T <: Txn[T]](val view: View[T], controls: ISeq[IControl[T]])
    extends View[T] with IControl[T] {

    type C = View.Component

    def component: C = view.component

    def initControl()(implicit tx: T): Unit =
      controls.foreach(_.initControl())

    def dispose()(implicit tx: T): Unit = {
      // N.B.: the view is also a control and thus will be disposed along with `controls`
//      view.dispose()
      controls.foreach(_.dispose())
    }
  }
}

final case class Graph(widget: Widget, controls: Vec[Control.Configured])
  extends expr.Graph {

  override def expand[T <: Txn[T]](implicit tx: T, ctx: Context[T]): View[T] with IControl[T] = {
    ctx.initGraph(this)
    val view: View[T] with IControl[T] = widget.expand[T]
    if (controls.isEmpty) view else {
      val disposables = controls.map(_.control.expand[T])
      new Graph.ExpandedImpl(view, disposables)
    }
  }
}