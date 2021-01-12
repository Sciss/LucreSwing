/*
 *  NumberValueExpandedImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
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

abstract class NumberValueExpandedImpl[T <: Txn[T], A, Repr[~ <: Txn[~]]](protected val view: Repr[T], value0: A)
                                                  (implicit protected val targets: ITargets[T], cursor: Cursor[T])
  extends IExpr[T, A]
    with IChangeGeneratorEvent[T, A]
    with TxnInit[T] {

  protected def viewState: A

  protected def guiInit(): Unit

  protected def guiDispose(): Unit

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

  private[this] var guiValue: A = _
  private[this] val txValue = Ref(value0)

  def value(implicit tx: T): A = txValue.get(tx.peer)

  def changed: IChangeEvent[T, A] = this

  private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): A =
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
