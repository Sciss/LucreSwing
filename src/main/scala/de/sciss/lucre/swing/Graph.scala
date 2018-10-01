/*
 *  Graph.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.expr
import de.sciss.lucre.expr.impl.{ExElem, GraphBuilderMixin, GraphSerializerMixin}
import de.sciss.lucre.expr.{Control, Ex}
import de.sciss.lucre.stm.{Cursor, Disposable, Obj, Sys, Workspace}
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.collection.immutable.{IndexedSeq => Vec, Seq => ISeq}
import scala.swing.Component

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

  implicit object serializer extends ImmutableSerializer[Graph] with GraphSerializerMixin {
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

  private final class ExpandedImpl[S <: Sys[S]](val view: View[S], controls: ISeq[Disposable[S#Tx]])
    extends View[S] {

    type C = scala.swing.Component

    def component: Component = view.component

    def dispose()(implicit tx: S#Tx): Unit = {
      // N.B.: the view is also a control and thus will be disposed along with `controls`
//      view.dispose()
      controls.foreach(_.dispose())
    }
  }
}

final case class Graph(widget: Widget, controls: Vec[Control.Configured])
  extends expr.Graph {

  def expand[S <: Sys[S]](self: Option[Obj[S]] = None)
                         (implicit tx: S#Tx, workspace: Workspace[S], cursor: Cursor[S]): View[S] = {
    implicit val ctx: Ex.Context[S] = Ex.Context(this, self.map(tx.newHandle(_)))
    val view: View[S] = widget.expand[S]
    if (controls.isEmpty) view else {
      val disp = controls.map(_.control.expand[S])
      new Graph.ExpandedImpl(view, disp)
    }
  }
}