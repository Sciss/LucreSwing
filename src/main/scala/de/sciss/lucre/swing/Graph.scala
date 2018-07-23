package de.sciss.lucre.swing

import java.util

import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys

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
}

final case class ConfiguredWidget(w: Widget, properties: Map[String, Any])

final case class Graph(widgets: Vec[ConfiguredWidget]) {
//  def isEmpty : Boolean  = sources.isEmpty
//  def nonEmpty: Boolean  = !isEmpty

  def expand[S <: Sys[S]](implicit tx: S#Tx, cursor: stm.Cursor[S]): View[S] = {
    implicit val b: Widget.Builder[S] = Widget.Builder(this)
    widgets.last.w.expand[S]
  }
}

