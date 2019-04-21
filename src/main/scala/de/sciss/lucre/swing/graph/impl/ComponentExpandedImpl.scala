/*
 *  ComponentExpandedImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.expr.{Ex, IControl}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.graph.Component
import de.sciss.lucre.swing.{View, deferTx}

trait ComponentExpandedImpl[S <: Sys[S]] extends View[S] with IControl[S] {

  protected def peer: Component

  private[this] var obs = List.empty[Disposable[S#Tx]]

  protected final def initProperty[A](key: String, default: A)(set: A => Unit)
                                     (implicit tx: S#Tx, ctx: Ex.Context[S]): Unit =
    ctx.getProperty[Ex[A]](peer, key) match {
      case Some(ex) =>
        val expr    = ex.expand[S]
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

  def initControl()(implicit tx: S#Tx): Unit = ()

  def initComponent()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
    initProperty(Component.keyEnabled   , Component.defaultEnabled  )(component.enabled   = _)
    initProperty(Component.keyFocusable , Component.defaultFocusable)(component.focusable = _)
    initProperty(Component.keyTooltip   , Component.defaultTooltip  )(component.tooltip   = _)
    this
  }

  def dispose()(implicit tx: S#Tx): Unit =
    obs.foreach(_.dispose())
}
