/*
 *  package.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre

import de.sciss.lucre.stm.TxnLike
import java.awt.EventQueue
import scala.swing.Swing
import scala.concurrent.stm.{TxnLocal, Txn}
import scala.util.control.NonFatal
import scala.collection.immutable.{IndexedSeq => Vec}

package object swing {
  private[this] val guiCode = TxnLocal(init = Vec.empty[() => Unit], afterCommit = handleGUI)

  private[this] def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit =
      seq.foreach { fun =>
        try {
          fun()
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }
      }

    defer(exec())
  }

  def requireEDT(): Unit = if (!EventQueue.isDispatchThread)
    throw new IllegalStateException("Should be called on the event dispatch thread")

  def defer(thunk: => Unit): Unit = {
    if (Txn.findCurrent.isDefined) throw new IllegalStateException("Trying to execute GUI code inside a transaction")
    if (EventQueue.isDispatchThread) thunk else Swing.onEDT(thunk)
  }

  def deferTx(thunk: => Unit)(implicit tx: TxnLike): Unit =
    guiCode.transform(_ :+ (() => thunk))(tx.peer)
}