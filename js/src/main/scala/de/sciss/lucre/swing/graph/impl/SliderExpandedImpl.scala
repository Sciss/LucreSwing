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

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L._
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Slider.{defaultMax, defaultMin, keyMax, keyMin}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

final class SliderExpandedImpl[T <: Txn[T]](peer: Slider, tx0: T)(implicit ctx: Context[T])
  extends NumberFieldExpandedImpl[T, Int, Slider](peer, tx0)
    with Slider.Repr[T]
    with ComponentHolder[L.Input] {

  override type C = View.Component

  def slider: View.Slider = component

  protected def input: L.Input = slider

  protected def keyValue    : String  = Slider.keyValue
  protected def defaultValue: Int     = Slider.defaultValue

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Int    ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Int    ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Int    ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)

    deferTx {
      val c = L.input(
        cls             := "lucre-slider",
        `type`          := "range",
        minAttr         := min.toString,
        maxAttr         := max.toString,
        L.defaultValue  := value0.toString,
      )

      component = c
    }

    initProperty(keyValue   , defaultValue    )(v => input.ref.valueAsNumber   = v)
    super.initComponent()
    this
  }

  protected def mkValueExpanded(value0: Int)(implicit tx: T, targets: ITargets[T],
                                             cursor: Cursor[T]): IExpr[T, Int] with TxnInit[T] =
    new SliderValueExpandedImpl[T](this, value0)
}
