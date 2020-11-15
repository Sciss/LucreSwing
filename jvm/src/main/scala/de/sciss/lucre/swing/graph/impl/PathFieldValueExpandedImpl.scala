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
import de.sciss.lucre.{Artifact, Cursor, ITargets, Txn}

import scala.swing.event.ValueChanged

final class PathFieldValueExpandedImpl[T <: Txn[T]](peer: => Peer, value0: Artifact.Value)
                                                   (implicit targets: ITargets[T], cursor: Cursor[T])
  extends ComponentPropertyExpandedImpl[T, Artifact.Value](value0) {

  protected def valueOnEDT: Artifact.Value =
    peer.valueOption match {
      case Some(f)  => f.toURI
      case None     => Artifact.Value.empty
    }

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
