/*
 *  PathFieldValueExpandedImpl.scala
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

import de.sciss.desktop.{PathField => Peer}
import de.sciss.file.File
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.IExpr
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.deferTx
import de.sciss.model.Change

import scala.concurrent.stm.Ref
import scala.swing.event.ValueChanged

final class PathFieldValueExpandedImpl[S <: Sys[S]](peer: => Peer, value0: File)
                                                   (implicit protected val targets: ITargets[S],
                                                    cursor: stm.Cursor[S])
  extends IExpr[S, File]
    with IGenerator[S, Change[File]] {

  private def commit(): Unit = {
    val c       = peer
    val before  = guiValue
    val now     = c.value
    val ch      = Change(before, now)
    if (ch.isSignificant) {
      guiValue    = now
      cursor.step { implicit tx =>
        txValue.set(now)(tx.peer)
        fire(ch)
      }
    }
  }

  private[this] var guiValue: File = _
  private[this] val txValue = Ref(value0)

  def value(implicit tx: S#Tx): File = txValue.get(tx.peer)

  def changed: IEvent[S, Change[File]] = this

  private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[File]] =
    Some(pull.resolve[Change[File]])

  def init()(implicit tx: S#Tx): this.type = {
    deferTx {
      val c = peer
      c.listenTo(c)
      c.reactions += {
        case ValueChanged(_) => commit()
      }
      guiValue = c.value
    }
    this
  }

  def dispose()(implicit tx: S#Tx): Unit = {
    deferTx {
      val c = peer
      c.deafTo(c)
    }
  }
}
