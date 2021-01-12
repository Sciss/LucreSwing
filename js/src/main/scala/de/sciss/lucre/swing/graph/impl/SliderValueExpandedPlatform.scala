/*
 *  SliderValueExpandedPlatform.scala
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
import de.sciss.lucre.Txn

trait SliderValueExpandedPlatform[T <: Txn[T]] extends IntFieldLikeValueExpandedPlatform[T] {
  protected def view: Slider.Repr[T]

  protected def input: L.Input = view.slider
}
