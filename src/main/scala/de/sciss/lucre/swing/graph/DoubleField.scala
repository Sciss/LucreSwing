/*
 *  DoubleField.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package graph

import java.text.{NumberFormat, ParseException}
import java.util.Locale

import de.sciss.audiowidgets.{ParamFormat, UnitView, ParamField => Peer}
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, ExSeq, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change
import de.sciss.numbers
import javax.swing.text.NumberFormatter

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.event.ValueChanged
import scala.util.Try

object DoubleField {
  def apply(): DoubleField = Impl()

  private final val keyValue        = "value"
  private final val keyMin          = "min"
  private final val keyMax          = "max"
  private final val keyDecimals     = "decimals"
  private final val keyStep         = "step"
  private final val keyUnit         = "unit"
  private final val keyPrototype    = "prototype"
  private final val keyEditable     = "editable"

  private final val defaultValue    = 0.0
  private final val defaultMin      = Double.NegativeInfinity
  private final val defaultMax      = Double.PositiveInfinity
  private final val defaultDecimals = 2
  private final val defaultStep     = 0.1
  private final val defaultUnit     = ""
  private final val defaultEditable = true

  final case class Value(w: DoubleField) extends Ex[Double] {
    override def productPrefix: String = s"DoubleField$$Value" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Double] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val valueOpt  = ctx.getProperty[Ex[Double]](w, keyValue)
      val value0    = valueOpt.fold[Double](defaultValue)(_.expand[S].value)
      new ValueExpanded[S](ws, value0).init()
    }
  }

  private final class ValueExpanded[S <: Sys[S]](ws: View.T[S, Peer[Double]], value0: Double)
                                                (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Double]
      with IGenerator[S, Change[Double]] {

    private def commit(): scala.Unit = {
      val c       = ws.component
      val before  = guiValue
      val now     = c.value
      val ch      = Change(before, now)
      if (ch.isSignificant) {
        guiValue    = now
        cursor.step { implicit tx =>
          txValue.set(now)(tx.peer)
          fire(ch)
        }
      }
    }

    private[this] var guiValue: Double = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): Double = txValue.get(tx.peer)

    def changed: IEvent[S, Change[Double]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Double]] =
      Some(pull.resolve[Change[Double]])

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        guiValue = c.value
        c.listenTo(c)
        c.reactions += {
          case ValueChanged(_) => commit()
        }
      }
      this
    }

    def dispose()(implicit tx: S#Tx): scala.Unit = {
      deferTx {
        val c = ws.component
        c.deafTo(c)
      }
    }
  }

  final case class Min(w: DoubleField) extends Ex[Double] {
    override def productPrefix: String = s"DoubleField$$Min" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Double] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyMin)
      valueOpt.getOrElse(Constant(defaultMin)).expand[S]
    }
  }

  final case class Max(w: DoubleField) extends Ex[Double] {
    override def productPrefix: String = s"DoubleField$$Max" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Double] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyMax)
      valueOpt.getOrElse(Constant(defaultMax)).expand[S]
    }
  }

  final case class Step(w: DoubleField) extends Ex[Double] {
    override def productPrefix: String = s"DoubleField$$Step" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Double] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyStep)
      valueOpt.getOrElse(Constant(defaultStep)).expand[S]
    }
  }

  final case class Decimals(w: DoubleField) extends Ex[Int] {
    override def productPrefix: String = s"DoubleField$$Decimals" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyDecimals)
      valueOpt.getOrElse(Constant(defaultDecimals)).expand[S]
    }
  }

  final case class Unit(w: DoubleField) extends Ex[String] {
    override def productPrefix: String = s"DoubleField$$Unit" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, String] = {
    val valueOpt = ctx.getProperty[Ex[String]](w, keyUnit)
        valueOpt.getOrElse(Constant(defaultUnit)).expand[S]
    }
  }

  private def defaultPrototype[S <: Sys[S]](w: DoubleField)(implicit ctx: Ex.Context[S]): Ex[ISeq[Double]] = {
    val seq0 = ctx.getProperty[Ex[Double]](w, keyValue).toList
    ExSeq(w.min :: w.max :: seq0: _*)
  }

  final case class Prototype(w: DoubleField) extends Ex[ISeq[Double]] {
    override def productPrefix: String = s"DoubleField$$Prototype" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, ISeq[Double]] = {
      val valueOpt = ctx.getProperty[Ex[ISeq[Double]]](w, keyPrototype)
      valueOpt.getOrElse(defaultPrototype(w)).expand[S]
    }
  }

  final case class Editable(w: DoubleField) extends Ex[Boolean] {
    override def productPrefix: String = s"DoubleField$$Editable" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Constant(defaultEditable)).expand[S]
    }
  }

  private final class Expanded[S <: Sys[S]](protected val w: DoubleField) extends View[S]
    with ComponentHolder[Peer[Double]] with ComponentExpandedImpl[S] {

    type C = Peer[Double]

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val value0    = ctx.getProperty[Ex[Double ]](w, keyValue    ).fold(defaultValue   )(_.expand[S].value)
      val min       = ctx.getProperty[Ex[Double ]](w, keyMin      ).fold(defaultMin     )(_.expand[S].value)
      val max       = ctx.getProperty[Ex[Double ]](w, keyMax      ).fold(defaultMax     )(_.expand[S].value)
      val step      = ctx.getProperty[Ex[Double ]](w, keyStep     ).fold(defaultStep    )(_.expand[S].value)
      val decimals  = ctx.getProperty[Ex[Int    ]](w, keyDecimals ).fold(defaultDecimals)(_.expand[S].value)
      val unitS     = ctx.getProperty[Ex[String ]](w, keyUnit     ).fold(defaultUnit    )(_.expand[S].value)
      val editable  = ctx.getProperty[Ex[Boolean]](w, keyEditable ).fold(defaultEditable)(_.expand[S].value)

      val prototype = ctx.getProperty[Ex[ISeq[Double]]](w, keyPrototype).getOrElse(defaultPrototype(w)).expand[S].value

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
        c.prototypeDisplayValues = prototype
        if (editable != defaultEditable) c.editable = editable
        component = c
      }

      initProperty(keyValue, defaultValue)(component.value = _)

      super.init()
    }
  }

  private final case class Impl() extends DoubleField with ComponentImpl { w =>
    override def productPrefix: String = "DoubleField" // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

    object value extends Model[Double] {
      def apply(): Ex[Double] = Value(w)

      def update(x: Ex[Double]): scala.Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValue, x)
      }
    }

    def min: Ex[Double] = Min(this)

    def min_=(x: Ex[Double]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMin, x)
    }

    def max: Ex[Double] = Max(this)

    def max_=(x: Ex[Double]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMax, x)
    }

    def step: Ex[Double] = Step(this)

    def step_=(x: Ex[Double]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyStep, x)
    }

    def decimals: Ex[Int] = Decimals(this)

    def decimals_=(x: Ex[Int]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyDecimals, x)
    }

    def editable: Ex[Boolean] = Editable(this)

    def editable_=(x: Ex[Boolean]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyEditable, x)
    }

    def unit: Ex[String] = Unit(this)

    def unit_=(x: Ex[String]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyUnit, x)
    }

    def prototype: Ex[ISeq[Double]] = Prototype(this)

    def prototype_=(x: Ex[ISeq[Double]]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyPrototype, x)
    }
  }
}
trait DoubleField extends NumberField[Double] {
  var decimals: Ex[Int]
}