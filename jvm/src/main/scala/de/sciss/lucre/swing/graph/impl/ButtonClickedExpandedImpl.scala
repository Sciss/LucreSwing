/*
 *  ButtonClickedExpandedImpl.scala
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

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.{Cursor, IEvent, IPull, ITargets, Txn}
import de.sciss.lucre.expr.ITrigger
import de.sciss.lucre.expr.graph.Trig
import de.sciss.lucre.impl.IGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx

final class ButtonClickedExpandedImpl[T <: Txn[T]](ws: View.T[T, scala.swing.Button])
                                                (implicit protected val targets: ITargets[T], cursor: Cursor[T])
  extends ITrigger[T] with IGeneratorEvent[T, Unit] {

  def changed: IEvent[T, Unit] = this

  private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[Unit] = Trig.Some

  private[this] lazy val listenerA = new ActionListener {
    def actionPerformed(e: ActionEvent): Unit =
      cursor.step { implicit tx =>
        fire(())
      }
  }

  def init()(implicit tx: T): this.type = {
    deferTx {
      val c = ws.component
      c.peer.addActionListener(listenerA)
    }
    this
  }

  def dispose()(implicit tx: T): Unit = {
    deferTx {
      val c = ws.component
      c.peer.removeActionListener(listenerA)
    }
  }
}