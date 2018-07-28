/*
 *  Widget.scala
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

import de.sciss.lucre.expr.impl.ContextMixin
import de.sciss.lucre.expr.{Ex, ExAttr}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Obj, Sys}

object Widget {
  object Builder {
    def apply[S <: Sys[S]](g: Graph, selfH: Option[stm.Source[S#Tx, Obj[S]]] = None)(implicit cursor: stm.Cursor[S]): Builder[S] =
      new Impl[S](g, selfH)

    private final class Impl[S <: Sys[S]](g: Graph, selfH: Option[stm.Source[S#Tx, Obj[S]]])
                                         (implicit val cursor: stm.Cursor[S])
      extends ContextMixin[S] with Builder[S] {

      private[this] val properties = new util.IdentityHashMap[Widget, Map[String, Any]]()

      g.widgets.foreach { c =>
        properties.put(c.w, c.properties)
      }

      def selfOption(implicit tx: S#Tx): Option[Obj[S]] = selfH.map(_.apply())

      def getProperty[A](w: Widget, key: String): Option[A] = {
        val m0 = properties.get(w)
        if (m0 == null) None else {
          m0.get(key).asInstanceOf[Option[A]]
        }
      }
    }
  }
  trait Builder[S <: Sys[S]] extends Ex.Context[S] {
    implicit def cursor: stm.Cursor[S]

    def getProperty[A](w: Widget, key: String): Option[A]
  }

  trait Model[A] {
    def apply(): Ex[A]
    def update(value: Ex[A]): Unit

    def <--> (attr: ExAttr.WithDefault[A]): Unit = {
      this <--- attr
      this ---> attr
    }

    def ---> (attr: ExAttr.Like[A]): Unit = {
      import WidgetOps._
      apply() ---> attr
    }

    def ---> (m: Model[A]): Unit =
      m <--- this

    def <--- (value: Ex[A]): Unit =
      update(value)

    def <--- (m: Model[A]): Unit =
      update(m())
  }
}
trait Widget extends Product {
  type C <: scala.swing.Component

  // this acts now as a fast unique reference
  @transient final private[this] lazy val ref = new AnyRef

  // ---- constructor ----
  Graph.builder.addWidget(this)

  final def expand[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
    b.visit[View.T[S, C]](ref, mkView)

  protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C]
}