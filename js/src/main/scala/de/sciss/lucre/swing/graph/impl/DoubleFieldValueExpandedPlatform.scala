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

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import org.scalajs.dom

trait DoubleFieldValueExpandedPlatform[T <: Txn[T]] /*extends DoubleFieldLikeValueExpandedPlatform[T]*/ {
  protected def view: DoubleField.Repr[T]

  protected def viewUpdated(): Unit

  protected def input: L.Input = view.doubleField

  protected def viewState: Double = try {
    input.ref.value.toDouble
  } catch {
    case _: Exception => 0
  }

  protected def guiInit(): Unit = {
    val c: Input = input
    val obs = Observer[dom.Event] { _ =>
      viewUpdated()
    }
    c.amend(
      onChange --> obs
    )
  }

  protected def guiDispose(): Unit = ()

}
