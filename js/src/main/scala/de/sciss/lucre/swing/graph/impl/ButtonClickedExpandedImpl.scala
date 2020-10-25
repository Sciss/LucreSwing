/*
 *  ButtonClickedExpandedImpl.scala
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

import com.raquo.laminar.api.L.{Button => Peer, _}
import de.sciss.lucre.expr.ITrigger
import de.sciss.lucre.expr.graph.Trig
import de.sciss.lucre.impl.IGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.{Cursor, IEvent, IPull, ITargets, Txn}
import org.scalajs.dom

final class ButtonClickedExpandedImpl[T <: Txn[T]](ws: View.T[T, Peer])
                                                  (implicit protected val targets: ITargets[T], cursor: Cursor[T])
  extends ITrigger[T] with IGeneratorEvent[T, Unit] {

  def changed: IEvent[T, Unit] = this

  private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[Unit] = Trig.Some

  def init()(implicit tx: T): this.type = {
    deferTx {
      val c = ws.component
      val obs = Observer[dom.MouseEvent] { _ =>
        cursor.step { implicit tx =>
          fire(())
        }
      }

      c.amend(
        onClick --> obs,
      )
    }
    this
  }

  def dispose()(implicit tx: T): Unit = {
/*
    deferTx {
      val c = ws.component
      ... // c.peer.removeActionListener(listenerA)
    }
*/
  }
}