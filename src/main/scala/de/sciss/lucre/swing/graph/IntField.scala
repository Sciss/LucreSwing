/*
 *  IntField.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import java.text.{NumberFormat, ParseException}
import java.util.Locale

import de.sciss.audiowidgets.{ParamFormat, UnitView, ParamField => Peer}
import de.sciss.lucre.event.impl.IChangeGenerator
import de.sciss.lucre.event.{IChangeEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, ExSeq, Graph, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change
import de.sciss.numbers
import javax.swing.text.NumberFormatter

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.event.ValueChanged
import scala.util.Try

object IntField {
  def apply(): IntField = Impl()

  private final val keyValue        = "value"
  private final val keyMin          = "min"
  private final val keyMax          = "max"
  private final val keyStep         = "step"
  private final val keyUnit         = "unit"
  private final val keyPrototype    = "prototype"
  private final val keyEditable     = "editable"

  private final val defaultValue    = 0
  private final val defaultMin      = Int.MinValue
  private final val defaultMax      = Int.MaxValue
  private final val defaultStep     = 1
  private final val defaultUnit     = ""
  private final val defaultEditable = true

  final case class Value(w: IntField) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"IntField$$Value" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue)
      val value0    = valueOpt.fold[Int](defaultValue)(_.expand[S].value)
      new ValueExpanded[S](ws, value0).init()
    }
  }

  private final class ValueExpanded[S <: Sys[S]](ws: View.T[S, Peer[Int]], value0: Int)
                                                (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Int]
      with IChangeGenerator[S, Int] {

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

    private[this] var guiValue: Int = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): Int = txValue.get(tx.peer)

    def changed: IChangeEvent[S, Int] = this

    private[lucre] def pullChange(pull: IPull[S])(implicit tx: S#Tx, phase: IPull.Phase): Int =
      pull.resolveExpr(this)

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

  final case class Min(w: IntField) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"IntField$$Min" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[S]
    }
  }

  final case class Max(w: IntField) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"IntField$$Max" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[S]
    }
  }

  final case class Step(w: IntField) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"IntField$$Step" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyStep)
      valueOpt.getOrElse(Const(defaultStep)).expand[S]
    }
  }

  final case class Unit(w: IntField) extends Ex[String] {
    type Repr[S <: Sys[S]] = IExpr[S, String]

    override def productPrefix: String = s"IntField$$Unit" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyUnit)
      valueOpt.getOrElse(Const(defaultUnit)).expand[S]
    }
  }

  private def defaultPrototype[S <: Sys[S]](w: IntField)(implicit ctx: Context[S], tx: S#Tx): Ex[Seq[Int]] = {
    val seq0 = ctx.getProperty[Ex[Int]](w, keyValue).toList
    ExSeq(w.min :: w.max :: seq0: _*)
  }

  final case class Prototype(w: IntField) extends Ex[Seq[Int]] {
    type Repr[S <: Sys[S]] = IExpr[S, Seq[Int]]

    override def productPrefix: String = s"IntField$$Prototype" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Seq[Int]]](w, keyPrototype)
      valueOpt.getOrElse(defaultPrototype(w)).expand[S]
    }
  }

  final case class Editable(w: IntField) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"IntField$$Editable" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[S]
    }
  }

  private final class Expanded[S <: Sys[S]](protected val peer: IntField) extends View[S]
    with ComponentHolder[Peer[Int]] with ComponentExpandedImpl[S] {

    type C = Peer[Int]

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val value0    = ctx.getProperty[Ex[Int    ]](peer, keyValue    ).fold(defaultValue   )(_.expand[S].value)
      val min       = ctx.getProperty[Ex[Int    ]](peer, keyMin      ).fold(defaultMin     )(_.expand[S].value)
      val max       = ctx.getProperty[Ex[Int    ]](peer, keyMax      ).fold(defaultMax     )(_.expand[S].value)
      val step      = ctx.getProperty[Ex[Int    ]](peer, keyStep     ).fold(defaultStep    )(_.expand[S].value)
      val unitS     = ctx.getProperty[Ex[String ]](peer, keyUnit     ).fold(defaultUnit    )(_.expand[S].value)
      val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[S].value)

      val prototype = ctx.getProperty[Ex[Seq[Int]]](peer, keyPrototype).getOrElse(defaultPrototype(peer)).expand[S].value

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
            import numbers.Implicits._
            val add = inc.toLong * step
//            val s: Int = math.signum(inc) * math.signum(step)
//            if (s == math.signum(add)) {  // detect overflow
              (in + add).clip(min, max).toInt
//            } else if (s < 0) min else max
          }
        }

        val c = new Peer[Int](value0, fmt :: Nil)
        c.prototypeDisplayValues = immutable(prototype)
        if (editable != defaultEditable) c.editable = editable
        component = c
      }

//      initProperty(keyMin   , defaultMin  )(component.min   = _)
      initProperty(keyValue , defaultValue)(component.value = _)

      super.initComponent()
    }
  }

  private def immutable[A](in: Seq[A]): ISeq[A] =
    in match {
      case ix: ISeq[A]  => ix
      case _            => in.toList
    }

  private final case class Impl() extends IntField with ComponentImpl { w =>
    override def productPrefix: String = "IntField" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

    object value extends Model[Int] {
      def apply(): Ex[Int] = Value(w)

      def update(x: Ex[Int]): scala.Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValue, x)
      }
    }

    def min: Ex[Int] = Min(this)

    def min_=(x: Ex[Int]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMin, x)
    }

    def max: Ex[Int] = Max(this)

    def max_=(x: Ex[Int]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMax, x)
    }

    def step: Ex[Int] = Step(this)

    def step_=(x: Ex[Int]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyStep, x)
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

    def prototype: Ex[Seq[Int]] = Prototype(this)

    def prototype_=(x: Ex[Seq[Int]]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyPrototype, x)
    }
  }
}
trait IntField extends NumberField[Int]