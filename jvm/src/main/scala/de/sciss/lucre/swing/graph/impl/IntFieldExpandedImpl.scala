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

import java.text.{NumberFormat, ParseException}
import java.util.Locale

import de.sciss.audiowidgets.{ParamFormat, UnitView}
import de.sciss.audiowidgets.{ParamField => Peer}
import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.IntField.{defaultEditable, defaultMax, defaultMin, defaultPrototype, defaultStep, defaultUnit, defaultValue, keyEditable, keyMax, keyMin, keyPrototype, keyStep, keyUnit, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.numbers
import javax.swing.text.NumberFormatter

import scala.collection.immutable.{Seq => ISeq}
import scala.util.Try

final class IntFieldExpandedImpl[T <: Txn[T]](protected val peer: IntField, tx0: T)(implicit ctx: Context[T])
  extends View[T]
  with ComponentHolder[View.IntField] with ComponentExpandedImpl[T] with IntField.Repr[T] {

  def intField: View.IntField = component

  def value: IExpr[T, Int] = _value

  private[this] val _value = {
    implicit val tx: T = tx0
    val valueOpt  = ctx.getProperty[Ex[Int]](peer, keyValue)
    val value0    = valueOpt.fold[Int](defaultValue)(_.expand[T].value)
    import ctx.{cursor, targets}
    new IntFieldValueExpandedImpl[T](this, value0)
  }

  private def immutable[A](in: Seq[A]): ISeq[A] =
    in match {
      case ix: ISeq[A]  => ix
      case _            => in.toList
    }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Int    ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Int    ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Int    ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)
    val step      = ctx.getProperty[Ex[Int    ]](peer, keyStep     ).fold(defaultStep    )(_.expand[T].value)
    val unitS     = ctx.getProperty[Ex[String ]](peer, keyUnit     ).fold(defaultUnit    )(_.expand[T].value)
//    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

    val prototype = ctx.getProperty[Ex[Seq[Int]]](peer, keyPrototype).getOrElse(defaultPrototype(peer)).expand[T].value

    deferTx {
      val fmt: ParamFormat[Int] = new ParamFormat[Int] {
        def unit: UnitView = if (unitS.isEmpty) UnitView.empty else UnitView(unitS, unitS)

        private[this] val numFmt = NumberFormat.getIntegerInstance(Locale.US)
        numFmt.setGroupingUsed(false)
        val formatter: NumberFormatter = new NumberFormatter(numFmt) {
          // override def valueToString(value: Any   ): String = format  (value.asInstanceOf[Long])
          override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
        }
        formatter.setMinimum(min)
        formatter.setMaximum(max)

        private def tryParse(s: String): Int =
          try {
            s.toInt
          } catch {
            case _: NumberFormatException => throw new ParseException(s, 0)
          }

        def parse(s: String): Option[Int] = Try(tryParse(s)).toOption

        def format(value: Int): String = formatter.valueToString(value)

        def adjust(in: Int, inc: Int): Int = {
          import numbers.LongFunctions.clip
          val add = inc.toLong * step
          //            val s: Int = math.signum(inc) * math.signum(step)
          //            if (s == math.signum(add)) {  // detect overflow
          clip(in + add, min, max).toInt
          //            } else if (s < 0) min else max
        }
      }

      val c = new Peer[Int](value0, fmt :: Nil)
      c.prototypeDisplayValues = immutable(prototype)
//      if (editable != defaultEditable) c.editable = editable
      component = c
    }

    //      initProperty(keyMin   , defaultMin  )(component.min   = _)
    initProperty(keyValue   , defaultValue    )(component.value     = _)
    initProperty(keyEditable, defaultEditable )(component.editable  = _)

    super.initComponent()

    _value.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    value.dispose()
  }
}