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

import java.util

import de.sciss.lucre.expr.impl.ExElem
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Obj, Sys}
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.collection.immutable.{IndexedSeq => Vec, Seq => ISeq}
import scala.swing.Component

object Graph {
  trait Builder {
    def addControl  (c: Control): Unit
    def addWidget   (w: Widget): Unit
    def putProperty (w: Widget, key: String, value: Any): Unit
  }

  /** This is analogous to `SynthGraph.Builder` in ScalaCollider. */
  def builder: Builder = builderRef.get()

  private[this] val builderRef: ThreadLocal[Builder] = new ThreadLocal[Builder] {
    override protected def initialValue: Builder = BuilderDummy
  }

  private[this] object BuilderDummy extends Builder {
    def addControl(c: Control ): Unit = ()
    def addWidget (x: Widget  ): Unit = ()

    def putProperty(w: Widget, key: String, value: Any): Unit = ()
  }

  def apply(thunk: => Widget): Graph = {
    val b   = new BuilderImpl
    val old = builderRef.get()
    builderRef.set(b)
    try {
      /* val leaf = */ thunk
      b.build()
    } finally {
      builderRef.set(old) // BuilderDummy
    }
  }

  private[this] final class BuilderImpl extends Builder {
    private[this] val widgets     = Vec.newBuilder[Widget ]
    private[this] val controls    = Vec.newBuilder[Control]
    private[this] val properties  = new util.IdentityHashMap[Widget, Map[String, Any]]()

    override def toString = s"lucre.swing.Graph.Builder@${hashCode.toHexString}"

    def build(): Graph = {
      val vecW = widgets  .result()
      val vecC = controls .result()
      val configured = vecW.map { w =>
        val m0 = properties.get(w)
        val m1 = if (m0 != null) m0 else Map.empty[String, Any]
        ConfiguredWidget(w, m1)
      }
      new Graph(configured, vecC)
    }

    def addWidget (g: Widget ): Unit = widgets  += g
    def addControl(c: Control): Unit = controls += c

    def putProperty(w: Widget, key: String, value: Any): Unit = {
      val m0 = properties.get(w)
      val m1 = if (m0 != null) m0 else Map.empty[String, Any]
      val m2 = m1 + (key -> value)
      properties.put(w, m2)
    }
  }

  implicit object serializer extends ImmutableSerializer[Graph] {
    private final val SER_VERSION = 0x5778  // "Wx"

    def write(g: Graph, out: DataOutput): Unit = {
      out.writeShort(SER_VERSION)
      var ref = null: ExElem.RefMapOut
      val wx = g.widgets
      out.writeInt(wx.size)
      wx.foreach { conf =>
        ref = ExElem.write(conf.w, out, ref)
        val m = conf.properties
        out.writeInt(m.size)
        m.foreach { case (key, v) =>
          out.writeUTF(key)
          ref = ExElem.write(v, out, ref)
        }
      }
      val cx = g.controls
      out.writeInt(cx.size)
      cx.foreach { ctl =>
        ref = ExElem.write(ctl, out, ref)
      }
    }

    def read(in: DataInput): Graph = {
      val cookie = in.readShort()
      require(cookie == SER_VERSION, s"Unexpected cookie $cookie")
      val szW = in.readInt()
      val wxb = Vec.newBuilder[ConfiguredWidget]
      wxb.sizeHint(szW)
      var i = 0
      val ref = new ExElem.RefMapIn
      while (i < szW) {
        val w = ExElem.read(in, ref).asInstanceOf[Widget]
        val mSz = in.readInt()
        val mb = Map.newBuilder[String, Any]
        mb.sizeHint(mSz)
        var j = 0
        while (j < mSz) {
          val k = in.readUTF()
          val v = ExElem.read(in, ref)
          mb += k -> v
          j += 1
        }
        val properties = mb.result()
        val configured = ConfiguredWidget(w, properties)
        wxb += configured
        i += 1
      }
      val szC = in.readInt()
      val cxb = Vec.newBuilder[Control]
      cxb.sizeHint(szC)
      i = 0
      while (i < szC) {
        val ctl = ExElem.read(in, ref).asInstanceOf[Control]
        cxb += ctl
        i += 1
      }
      new Graph(wxb.result(), cxb.result())
    }
  }

//  trait Expanded[S <: Sys[S]] extends Disposable[S#Tx] {
//    def view: View[S]
//  }

  private final class ExpandedImpl[S <: Sys[S]](val view: View[S], controls: ISeq[Disposable[S#Tx]])
    extends View[S] {

    type C = scala.swing.Component

    def component: Component = view.component

    def dispose()(implicit tx: S#Tx): Unit = {
      view.dispose()
      controls.foreach(_.dispose())
    }
  }
}

final case class ConfiguredWidget(w: Widget, properties: Map[String, Any])

final case class Graph(widgets: Vec[ConfiguredWidget], controls: Vec[Control]) {
  def expand[S <: Sys[S]](self: Option[Obj[S]] = None)(implicit tx: S#Tx, cursor: stm.Cursor[S]): View[S] = {
    implicit val b: Widget.Builder[S] = Widget.Builder(this, self.map(tx.newHandle(_)))
    val view: View[S] = widgets.last.w.expand[S]
    if (controls.isEmpty) view else {
      val disp = controls.map(_.expand[S])
      new Graph.ExpandedImpl(view, disp)
    }
  }
}