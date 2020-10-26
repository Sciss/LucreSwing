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
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Slider.{defaultMax, defaultMin, defaultValue, keyMax, keyMin, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{IExpr, Txn}

final class SliderExpandedImpl[T <: Txn[T]](protected val peer: Slider, tx0: T)(implicit ctx: Context[T])
  extends View[T]
  with ComponentHolder[View.Slider] with ComponentExpandedImpl[T] with Slider.Repr[T] {

  def slider: View.Slider = component

  def value: IExpr[T, Int] = _value

  private[this] val _value = {
    implicit val tx: T = tx0
    val valueOpt = ctx.getProperty[Ex[Int]](peer, keyValue)
    val value0   = valueOpt.fold[Int](defaultValue)(_.expand[T].value)
    import ctx.{cursor, targets}
    new SliderValueExpandedImpl[T](this, value0)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Int    ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Int    ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Int    ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)

    deferTx {
      val c     = new scala.swing.Slider
      c.min     = min
      c.max     = max
      c.value   = value0
      component = c
    }

    initProperty(keyValue   , defaultValue    )(component.value     = _)
    super.initComponent()

    _value.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    value.dispose()
  }
}
