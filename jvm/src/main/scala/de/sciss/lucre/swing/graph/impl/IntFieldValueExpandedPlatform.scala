/*
 *  IntFieldValueExpandedPlatform.scala
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

import de.sciss.lucre.Txn

import scala.swing.event.ValueChanged

trait IntFieldValueExpandedPlatform[T <: Txn[T]] {
  protected def view: IntField.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Int = view.intField.value

  protected def guiInit(): Unit = {
    val c = view.intField
    c.listenTo(c)
    c.reactions += {
      case ValueChanged(_) => viewUpdated()
    }
  }

  protected def guiDispose(): Unit = {
    val c = view.intField
    c.deafTo(c)
  }
}
