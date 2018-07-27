package de.sciss.lucre.swing

import java.util

import de.sciss.lucre.expr.impl.ExElem
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Obj, Sys}
import de.sciss.serial.{DataInput, DataOutput, ImmutableSerializer}

import scala.collection.immutable.{IndexedSeq => Vec}

object Graph {
  trait Builder {
    def addWidget(w: Widget): Unit
    def putProperty(w: Widget, key: String, value: Any): Unit
  }

  /** This is analogous to `SynthGraph.Builder` in ScalaCollider. */
  def builder: Builder = builderRef.get()

  private[this] val builderRef: ThreadLocal[Builder] = new ThreadLocal[Builder] {
    override protected def initialValue: Builder = BuilderDummy
  }

  private[this] object BuilderDummy extends Builder {
    def addWidget(x: Widget): Unit = ()

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
    private[this] val widgets     = Vec.newBuilder[Widget]
    private[this] val properties  = new util.IdentityHashMap[Widget, Map[String, Any]]()

    override def toString = s"lucre.swing.Graph.Builder@${hashCode.toHexString}"

    def build(): Graph = {
      val vec = widgets.result()
      val configured = vec.map { w =>
        val m0 = properties.get(w)
        val m1 = if (m0 != null) m0 else Map.empty[String, Any]
        ConfiguredWidget(w, m1)
      }
      new Graph(configured)
    }

    def addWidget(g: Widget): Unit = widgets += g

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
      val wx = g.widgets
      out.writeInt(wx.size)
      var ref = null: ExElem.RefMapOut
      wx.foreach { conf =>
        ref = ExElem.write(conf.w, out, ref)
        val m = conf.properties
        out.writeInt(m.size)
        m.foreach { case (key, v) =>
          out.writeUTF(key)
          ref = ExElem.write(v, out, ref)
        }
      }
    }

    def read(in: DataInput): Graph = {
      val cookie = in.readShort()
      require(cookie == SER_VERSION, s"Unexpected cookie $cookie")
      val sz = in.readInt()
      val wxb = Vec.newBuilder[ConfiguredWidget]
      wxb.sizeHint(sz)
      var i = 0
      val ref = new ExElem.RefMapIn
      while (i < sz) {
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
      new Graph(wxb.result())
    }
  }
}

final case class ConfiguredWidget(w: Widget, properties: Map[String, Any])

final case class Graph(widgets: Vec[ConfiguredWidget]) {
//  def isEmpty : Boolean  = sources.isEmpty
//  def nonEmpty: Boolean  = !isEmpty

  def expand[S <: Sys[S]](self: Option[Obj[S]] = None)(implicit tx: S#Tx, cursor: stm.Cursor[S]): View[S] = {
    implicit val b: Widget.Builder[S] = Widget.Builder(this, self.map(tx.newHandle(_)))
    widgets.last.w.expand[S]
  }
}

