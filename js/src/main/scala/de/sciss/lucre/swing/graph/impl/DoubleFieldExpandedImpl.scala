/*
 *  DoubleFieldExpandedImpl.scala
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
import de.sciss.lucre.swing.graph.DoubleField.{defaultEditable, defaultMax, defaultMin, defaultStep, defaultUnit, keyEditable, keyMax, keyMin, keyStep, keyUnit}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

final class DoubleFieldExpandedImpl[T <: Txn[T]](peer: DoubleField, tx0: T)(implicit ctx: Context[T])
  extends NumberFieldExpandedImpl[T, Double, DoubleField](peer, tx0)
    with DoubleField.Repr[T]
    with ComponentHolder[L.HtmlElement] {

  override type C = View.Component

  var doubleField: View.DoubleField = _

  protected def input: L.Input = doubleField

  protected def keyValue    : String  = Slider.keyValue
  protected def defaultValue: Double  = Slider.defaultValue

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Double ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Double ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Double ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)
    // XXX TODO: setting step will disallow finer grained decimal values
//    val step      = ctx.getProperty[Ex[Double ]](peer, keyStep     ).fold(defaultStep    )(_.expand[T].value)
    val unitS     = ctx.getProperty[Ex[String ]](peer, keyUnit     ).fold(defaultUnit    )(_.expand[T].value)
    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

    deferTx {
      val c = L.input(
        cls             := "lucre-double-field",
        `type`          := "number",
        minAttr         := min.toString,
        maxAttr         := max.toString,
        stepAttr        := "0.00001", // step.toString,
        L.defaultValue  := value0.toString,
        contentEditable := editable,
      )

      val el = if (unitS.isEmpty) c else
        span(
          c,
          unitS,
          cls := "lucre-double-field",
        )

      component   = el
      doubleField = c
    }

    super.initComponent()
  }

  protected def mkValueExpanded(value0: Double)(implicit tx: T, targets: ITargets[T],
                                                cursor: Cursor[T]): IExpr[T, Double] with TxnInit[T] =
    new DoubleFieldValueExpandedImpl[T](this, value0)
}
