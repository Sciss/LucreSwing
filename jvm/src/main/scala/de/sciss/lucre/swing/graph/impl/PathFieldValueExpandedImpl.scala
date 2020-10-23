/*
 *  PathFieldValueExpandedImpl.scala
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

import de.sciss.desktop.{PathField => Peer}
import de.sciss.file.File
import de.sciss.lucre.{Cursor, ITargets, Txn}

import scala.swing.event.ValueChanged

final class PathFieldValueExpandedImpl[T <: Txn[T]](peer: => Peer, value0: File)
                                                   (implicit targets: ITargets[T], cursor: Cursor[T])
  extends ComponentPropertyExpandedImpl[T, File](value0) {

  protected def valueOnEDT: File = peer.value

  protected def startListening(): Unit = {
    val c = peer
    c.listenTo(c)
    c.reactions += {
      case ValueChanged(_) => commit()
    }
  }

  protected def stopListening(): Unit = {
    val c = peer
    c.deafTo(c)
  }
}