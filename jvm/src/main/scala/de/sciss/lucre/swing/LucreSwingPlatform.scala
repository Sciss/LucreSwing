/*
 *  LucreSwingPlatform.scala
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

import java.awt.EventQueue

import de.sciss.lucre.swing.graph.DropTarget

import scala.concurrent.stm.Txn
import scala.swing.Swing

trait LucreSwingPlatform {
  protected def initPlatform(): Unit = _initPlatform

  private lazy val _initPlatform: Unit = {
    DropTarget.init()
  }

  def requireEDT(): Unit = if (!EventQueue.isDispatchThread)
    throw new IllegalStateException("Should be called on the event dispatch thread")

  def defer(thunk: => Unit): Unit = {
    if (Txn.findCurrent.isDefined) throw new IllegalStateException("Trying to execute GUI code inside a transaction")
    if (EventQueue.isDispatchThread) thunk else Swing.onEDT(thunk)
  }
}
