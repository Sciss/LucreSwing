/*
 *  SliderExpandedImpl.scala
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

import de.sciss.lucre.expr.Context
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

final class SliderExpandedImpl[T <: Txn[T]](peer: Slider, tx0: T)(implicit ctx: Context[T])
  extends IntFieldLikeExpandedImpl[T, Slider](peer, tx0)
    with Slider.Repr[T] {

  override type C = View.Component

  def slider: View.Slider = component

  protected def inputType : String = "range"
  protected def cssClass  : String = "lucre-slider"

  protected def mkValueExpanded(value0: Int)(implicit tx: T, targets: ITargets[T],
                                             cursor: Cursor[T]): IExpr[T, Int] with TxnInit[T] =
    new SliderValueExpandedImpl[T](this, value0)
}
