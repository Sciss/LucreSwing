/*
 *  ComboBoxValueExpandedImpl.scala
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

import com.raquo.laminar.api.L._
import de.sciss.lucre.{Cursor, ITargets, Txn}
import org.scalajs.dom

final class ComboBoxValueExpandedImpl[T <: Txn[T], A](peer: => View.ComboBox[A], items: Seq[A],
                                                      value0: (Int, Option[A]))
                                                     (implicit targets: ITargets[T], cursor: Cursor[T])
  extends ComponentPropertyExpandedImpl[T, (Int, Option[A])](value0) {

  protected def valueOnEDT: (Int, Option[A]) = {
    val s       = peer.ref
    val idx     = s.selectedIndex
    val valOpt  = if (idx < 0 || idx > items.size) None else Some(items(idx))
    (idx, valOpt)
  }

  protected def startListening(): Unit = {
    val c = peer
    val obs = Observer[dom.Event] { _ =>
      commit()
    }
    c.amend(
      onChange --> obs
    )
  }

  protected def stopListening(): Unit = ()
}
