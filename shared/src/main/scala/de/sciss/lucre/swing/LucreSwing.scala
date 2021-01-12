/*
 *  LucreSwing.scala
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

import de.sciss.lucre.Log.{swing => log}
import de.sciss.lucre.TxnLike

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.TxnLocal
import scala.util.control.NonFatal

object LucreSwing extends LucreSwingPlatform {
  /** Registers all known types. */
  def init(): Unit = initPlatform()

  private[this] val guiCode = TxnLocal(init = Vec.empty[() => Unit], afterCommit = handleGUI)

  private[this] def handleGUI(seq: Vec[() => Unit]): Unit = {
    def exec(): Unit = {
      log.debug(s"handleGUI(seq.size = ${seq.size})")
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

  def deferTx(thunk: => Unit)(implicit tx: TxnLike): Unit =
    guiCode.transform(_ :+ (() => thunk))(tx.peer)
}
