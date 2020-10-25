/*
 *  IntFieldLikeExpandedImpl.scala
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
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Component.{defaultEnabled, keyEnabled}
import de.sciss.lucre.swing.graph.Slider.{defaultMax, defaultMin, defaultValue, keyMax, keyMin, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

abstract class IntFieldLikeExpandedImpl[T <: Txn[T], P <: Component](protected val peer: P, tx0: T)(implicit ctx: Context[T])
  extends View[T] with IControl[T]
    with ComponentHolder[L.Input] with ComponentExpandedImpl[T] {

  type C = View.Component

  def value: IExpr[T, Int] = _value

  protected def mkValueExpanded(value0: Int)(implicit tx: T, targets: ITargets[T],
                                             cursor: Cursor[T]): IExpr[T, Int] with TxnInit[T]

  protected def inputType: String
  protected def cssClass : String

  private[this] val _value = {
    implicit val tx: T = tx0
    val valueOpt = ctx.getProperty[Ex[Int]](peer, keyValue)
    val value0   = valueOpt.fold[Int](defaultValue)(_.expand[T].value)
    import ctx.{cursor, targets}
    mkValueExpanded(value0)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    deferTx {
      val c = input(
        cls     := cssClass,
        `type`  := inputType,
      )

      component = c
    }

    initProperty(keyMin     , defaultMin    )(v => component.ref.min      = v.toString)
    initProperty(keyMax     , defaultMax    )(v => component.ref.max      = v.toString)
    initProperty(keyValue   , defaultValue  )(v => component.ref.value    = v.toString)
    initProperty(keyEnabled , defaultEnabled)(v => component.ref.disabled = !v)

    super.initComponent()

    _value.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    value.dispose()
  }
}
