/*
 *  ComponentPropertyExpandedImpl.scala
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

import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
import de.sciss.model.Change

import scala.concurrent.stm.Ref

/** Make sure to call `init()`! */
abstract class ComponentPropertyExpandedImpl[T <: Txn[T], A](value0: A)
                                                            (implicit protected val targets: ITargets[T],
                                                             cursor: Cursor[T])
  extends IExpr[T, A]
    with IChangeGeneratorEvent[T, A] {

  // ---- abstract ----

  /** Called on the EDT. Implementation should register listeners for the property here. */
  protected def startListening(): Unit

  /** Called on the EDT. Implementation should unregister listeners for the property here. */
  protected def stopListening (): Unit

  /** Called on the EDT. Read the current property value. */
  protected def valueOnEDT: A

  // ---- impl ----

  final protected def commit(): Unit = {
    val before  = _guiValue
    val now     = valueOnEDT
    val ch      = Change(before, now)
    if (ch.isSignificant) {
      _guiValue    = now
      cursor.step { implicit tx =>
        txValue.set(now)(tx.peer)
        fire(ch)
      }
    }
  }

  private[this] var _guiValue: A = _
  private[this] val txValue = Ref[A](value0)

  def value(implicit tx: T): A = txValue.get(tx.peer)

  def changed: IChangeEvent[T, A] = this

  private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): A =
    pull.resolveExpr(this)

  def init()(implicit tx: T): this.type = {
    deferTx {
      startListening()
      _guiValue = valueOnEDT
    }
    this
  }

  def dispose()(implicit tx: T): Unit = {
    deferTx {
      stopListening()
    }
  }
}
