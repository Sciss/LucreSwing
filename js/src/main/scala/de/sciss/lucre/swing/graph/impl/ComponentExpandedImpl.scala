/*
 *  ComponentExpandedImpl.scala
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

package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.Component
import de.sciss.lucre.{Disposable, Txn}

import com.raquo.laminar.api.L.{_}

trait ComponentExpandedImpl[T <: Txn[T]] extends View[T] with IControl[T] {

  protected def peer: Component

  private[this] var obs = List.empty[Disposable[T]]

  protected final def initProperty[A](key: String, default: A)(set: A => Unit)
                                     (implicit tx: T, ctx: Context[T]): Unit =
    ctx.getProperty[Ex[A]](peer, key) match {
      case Some(ex) =>
        val expr    = ex.expand[T]
        val value0  = expr.value
        if (value0 != default) deferTx {
          set(value0)
        }
        obs ::= expr.changed.react { implicit tx => upd =>
          deferTx {
            set(upd.now)
          }
        }

      case _ =>
    }

  def initControl()(implicit tx: T): Unit = ()

  def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
//    initProperty(Component.keyEnabled   , Component.defaultEnabled  )(p => component.amend(disabled := !p))
//    initProperty(Component.keyFocusable , Component.defaultFocusable)(component.focusable = _)
//    initProperty(Component.keyTooltip   , Component.defaultTooltip  )(component.tooltip   = _)
    this
  }

  def dispose()(implicit tx: T): Unit =
    obs.foreach(_.dispose())
}
