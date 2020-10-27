/*
 *  IntFieldExpandedImpl.scala
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
import de.sciss.lucre.swing.graph.IntField.{defaultEditable, defaultMax, defaultMin, defaultStep, defaultUnit, keyEditable, keyMax, keyMin, keyStep, keyUnit}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

final class IntFieldExpandedImpl[T <: Txn[T]](peer: IntField, tx0: T)(implicit ctx: Context[T])
  extends NumberFieldExpandedImpl[T, Int, IntField](peer, tx0)
    with IntField.Repr[T]
    with ComponentHolder[L.HtmlElement] {

  override type C = View.Component

  var intField: View.IntField = _

  protected def input: L.Input = intField

  protected def keyValue    : String  = IntField.keyValue
  protected def defaultValue: Int     = IntField.defaultValue

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Int    ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Int    ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Int    ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)
    val step      = ctx.getProperty[Ex[Int    ]](peer, keyStep     ).fold(defaultStep    )(_.expand[T].value)
    val unitS     = ctx.getProperty[Ex[String ]](peer, keyUnit     ).fold(defaultUnit    )(_.expand[T].value)
    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

    deferTx {
      val c = L.input(
        cls             := "lucre-int-field",
        `type`          := "number",
        minAttr         := min.toString,
        maxAttr         := max.toString,
        stepAttr        := step.toString,
        L.defaultValue  := value0.toString,
        contentEditable := editable,
      )

      val el = if (unitS.isEmpty) c else
        span(
          c,
          cls := "lucre-int-field",
          span(
            cls := "lucre-unit",
            unitS,
          ),
        )

      component = el
      intField  = c
    }

    initProperty(keyValue   , defaultValue    )(v => input.ref.valueAsNumber   = v)
    initProperty(keyEditable, defaultEditable )(v => input.ref.contentEditable = v.toString)
    super.initComponent()
  }

  protected def mkValueExpanded(value0: Int)(implicit tx: T, targets: ITargets[T],
                                             cursor: Cursor[T]): IExpr[T, Int] with TxnInit[T] =
    new IntFieldValueExpandedImpl[T](this, value0)
}
