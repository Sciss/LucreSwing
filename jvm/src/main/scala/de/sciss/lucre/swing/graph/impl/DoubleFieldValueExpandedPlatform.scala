/*
 *  DoubleFieldValueExpandedPlatform.scala
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

import de.sciss.lucre.Txn

import scala.swing.event.ValueChanged

trait DoubleFieldValueExpandedPlatform[T <: Txn[T]] {
  protected def view: DoubleField.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Double = view.doubleField.value

  protected def guiInit(): Unit = {
    val c = view.doubleField
    c.listenTo(c)
    c.reactions += {
      case ValueChanged(_) => viewUpdated()
    }
  }

  protected def guiDispose(): Unit = {
    val c = view.doubleField
    c.deafTo(c)
  }
}
