/*
 *  ComboBoxValueExpandedImpl.scala
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

import de.sciss.lucre.event.ITargets
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.swingplus.{ComboBox => Peer}

import scala.swing.event.SelectionChanged

final class ComboBoxValueExpandedImpl[S <: Sys[S], A](peer: => Peer[A], value0: A)
                                                     (implicit targets: ITargets[S], cursor: stm.Cursor[S])
  extends ComponentPropertyExpandedImpl[S, A](value0) {

  protected def valueOnEDT: A = Option(peer.selection.item).getOrElse(value0)

  protected def startListening(): Unit = {
    val c = peer
    c.listenTo(c.selection)
    c.reactions += {
      case SelectionChanged(_) => commit()
    }
  }

  protected def stopListening(): Unit = {
    val c = peer
    // N.B.: this will stop delivering events to _any_ listener,
    // however `dispose()` will be called for the entire graph,
    // so that is not a problem
    c.deafTo(c.selection)
  }
}
