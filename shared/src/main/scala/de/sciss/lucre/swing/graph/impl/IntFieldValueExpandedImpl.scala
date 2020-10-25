/*
 *  IntFieldValueExpandedImpl.scala
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
package graph
package impl

import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.model.Change

import scala.concurrent.stm.Ref

// XXX TODO DRY with SliderValueExpandedImpl
final class IntFieldValueExpandedImpl[T <: Txn[T]](protected val view: IntField.Repr[T], value0: Int)
                                              (implicit protected val targets: ITargets[T], cursor: Cursor[T])
  extends IExpr[T, Int]
    with IChangeGeneratorEvent[T, Int]
    with IntFieldValueExpandedPlatform[T]
    with TxnInit[T] {

  protected def viewUpdated(): Unit = {
    val before  = guiValue
    val now     = viewState
    val ch      = Change(before, now)
    if (ch.isSignificant) {
      guiValue    = now
      cursor.step { implicit tx =>
        txValue.set(now)(tx.peer)
        fire(ch)
      }
    }
  }

  private[this] var guiValue: Int = _
  private[this] val txValue = Ref(value0)

  def value(implicit tx: T): Int = txValue.get(tx.peer)

  def changed: IChangeEvent[T, Int] = this

  private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): Int =
    pull.resolveExpr(this)

  def init()(implicit tx: T): this.type = {
    deferTx {
      guiInit()
      guiValue = viewState
    }
    this
  }

  def dispose()(implicit tx: T): scala.Unit = {
    deferTx {
      guiDispose()
    }
  }
}
