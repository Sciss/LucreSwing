/*
 *  DoubleFieldExpandedImpl.scala
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

import java.text.{NumberFormat, ParseException}
import java.util.Locale

import de.sciss.audiowidgets.{ParamFormat, UnitView}
import de.sciss.audiowidgets.{ParamField => Peer}
import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.DoubleField.{defaultDecimals, defaultEditable, defaultMax, defaultMin, defaultPrototype, defaultStep, defaultUnit, defaultValue, keyDecimals, keyEditable, keyMax, keyMin, keyPrototype, keyStep, keyUnit, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.numbers
import javax.swing.text.NumberFormatter

import scala.collection.immutable.{Seq => ISeq}
import scala.util.Try

final class DoubleFieldExpandedImpl[T <: Txn[T]](protected val peer: DoubleField, tx0: T)(implicit ctx: Context[T])
  extends View[T]
    with ComponentHolder[View.DoubleField] with ComponentExpandedImpl[T] with DoubleField.Repr[T] {

  def doubleField: View.DoubleField = component

  def value: IExpr[T, Double] = _value

  private[this] val _value = {
    implicit val tx: T = tx0
    val valueOpt  = ctx.getProperty[Ex[Double]](peer, keyValue)
    val value0    = valueOpt.fold[Double](defaultValue)(_.expand[T].value)
    import ctx.{cursor, targets}
    new DoubleFieldValueExpandedImpl[T](this, value0)
  }

  private def immutable[A](in: Seq[A]): ISeq[A] =
    in match {
      case ix: ISeq[A]  => ix
      case _            => in.toList
    }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val value0    = ctx.getProperty[Ex[Double ]](peer, keyValue    ).fold(defaultValue   )(_.expand[T].value)
    val min       = ctx.getProperty[Ex[Double ]](peer, keyMin      ).fold(defaultMin     )(_.expand[T].value)
    val max       = ctx.getProperty[Ex[Double ]](peer, keyMax      ).fold(defaultMax     )(_.expand[T].value)
    val step      = ctx.getProperty[Ex[Double ]](peer, keyStep     ).fold(defaultStep    )(_.expand[T].value)
    val unitS     = ctx.getProperty[Ex[String ]](peer, keyUnit     ).fold(defaultUnit    )(_.expand[T].value)
//    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)
    val decimals  = ctx.getProperty[Ex[Int    ]](peer, keyDecimals ).fold(defaultDecimals)(_.expand[T].value)

    val prototype = ctx.getProperty[Ex[Seq[Double]]](peer, keyPrototype).getOrElse(defaultPrototype(peer)).expand[T].value

    deferTx {
      val fmt: ParamFormat[Double] = new ParamFormat[Double] {
        def unit: UnitView = if (unitS.isEmpty) UnitView.empty else UnitView(unitS, unitS)

        private[this] val numFmt = NumberFormat.getIntegerInstance(Locale.US)
        numFmt.setGroupingUsed(false)
        numFmt.setMinimumFractionDigits(decimals)
        numFmt.setMaximumFractionDigits(decimals)
        val formatter: NumberFormatter = new NumberFormatter(numFmt) {
          // override def valueToString(value: Any   ): String = format  (value.asInstanceOf[Long])
          override def stringToValue(text : String): AnyRef = tryParse(text).asInstanceOf[AnyRef]
        }
        formatter.setMinimum(min)
        formatter.setMaximum(max)

        private def tryParse(s: String): Double =
          try {
            s.toDouble
          } catch {
            case _: NumberFormatException => throw new ParseException(s, 0)
          }

        def parse(s: String): Option[Double] = Try(tryParse(s)).toOption

        def format(value: Double): String = formatter.valueToString(value)

        def adjust(in: Double, inc: Int): Double = {
          import numbers.Implicits._
          val add = inc * step
          (in + add).clip(min, max)
        }
      }

      val c = new Peer[Double](value0, fmt :: Nil)
      c.prototypeDisplayValues = immutable(prototype)
//      if (editable != defaultEditable) c.editable = editable
      component = c
    }

//    initProperty(keyMin   , defaultMin  )(component.min   = _)
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