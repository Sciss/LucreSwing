/*
 *  LucreSwing.scala
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

import java.awt.EventQueue
import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.lucre.stm.TxnLike
import de.sciss.lucre.swing.graph.DropTarget

import scala.annotation.elidable
import scala.annotation.elidable.CONFIG
import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.{Txn, TxnLocal}
import scala.swing.Swing
import scala.util.control.NonFatal

object LucreSwing {
  /** Registers all known types. */
  def init(): Unit = _init

  private lazy val _init: Unit = {
    DropTarget.init()
  }

  private[this] val guiCode = TxnLocal(init = Vec.empty[() => Unit], afterCommit = handleGUI)

  private[this] def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit = {
      log(s"handleGUI(seq.size = ${seq.size})")
      seq.foreach { fun =>
        try {
          fun()
        } catch {
          case NonFatal(e) => e.printStackTrace()
        }
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

  private[this] lazy val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'Lucre' - 'swing' ", Locale.US)
  var showLog = false

  @elidable(CONFIG) private[lucre] def log(what: => String): Unit =
    if (showLog) println(logHeader.format(new Date()) + what)
}
