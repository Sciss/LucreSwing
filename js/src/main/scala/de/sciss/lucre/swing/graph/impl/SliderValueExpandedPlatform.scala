/*
 *  SliderValueExpandedPlatform.scala
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

import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import org.scalajs.dom

trait SliderValueExpandedPlatform[T <: Txn[T]] {
  protected def view: Slider.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Int = try {
    view.slider.ref.value.toInt
  } catch {
    case _: Exception => 0
  }

  protected def guiInit(): Unit = {
    val c: Input = view.slider
    val obs = Observer[dom.Event] { _ =>
      viewUpdated()
    }
    c.amend(
      onChange --> obs
    )
  }

  protected def guiDispose(): Unit = ()
}
