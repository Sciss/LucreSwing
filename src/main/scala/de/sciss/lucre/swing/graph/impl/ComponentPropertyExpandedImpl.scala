/*
 *  ComponentPropertyExpandedImpl.scala
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

import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.IExpr
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.model.Change

import scala.concurrent.stm.Ref

/** Make sure to call `init()`! */
abstract class ComponentPropertyExpandedImpl[S <: Sys[S], A](value0: A)
                                                            (implicit protected val targets: ITargets[S],
                                                             cursor: stm.Cursor[S])
  extends IExpr[S, A]
    with IGenerator[S, Change[A]] {

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

  def value(implicit tx: S#Tx): A = txValue.get(tx.peer)

  def changed: IEvent[S, Change[A]] = this

  private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[A]] =
    Some(pull.resolve)

  def init()(implicit tx: S#Tx): this.type = {
    deferTx {
      startListening()
      _guiValue = valueOnEDT
    }
    this
  }

  def dispose()(implicit tx: S#Tx): Unit = {
    deferTx {
      stopListening()
    }
  }
}
