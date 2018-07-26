/*
 *  ComponentExpandedImpl.scala
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
package graph
package impl

import de.sciss.lucre.expr.Ex
import de.sciss.lucre.stm.{Disposable, Sys}

trait ComponentExpandedImpl[S <: Sys[S]] {
  _: View[S] =>

  protected def w: Component

  private[this] var obs: Disposable[S#Tx] = _

  def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
    b.getProperty[Ex[Boolean]](w, Component.keyEnabled) match {
      case Some(ex) /* if ex != Constant(Component.keyEnabled) */ =>
        val expr    = ex.expand[S]
        val value0  = expr.value
        if (value0 != Component.defaultEnabled) deferTx {
          component.peer.setEnabled(value0)
        }
        obs = expr.changed.react { implicit tx => upd =>
          deferTx {
            component.peer.setEnabled(upd.now)
          }
        }

      case _ =>
    }
    this
  }

  def dispose()(implicit tx: S#Tx): Unit = {
    if (obs != null) obs.dispose()
  }
}
