/*
 *  DoubleField.scala
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

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, ExSeq, Graph, IControl, Model}
import de.sciss.lucre.swing.graph.impl.{ComponentImpl, DoubleFieldExpandedImpl, DoubleFieldValueExpandedImpl}
import de.sciss.lucre.swing.View
import de.sciss.lucre.{IExpr, Txn}

object DoubleField {
  def apply(): DoubleField = Impl()

  private[graph] final val keyValue        = "value"
  private[graph] final val keyMin          = "min"
  private[graph] final val keyMax          = "max"
  private[graph] final val keyDecimals     = "decimals"
  private[graph] final val keyStep         = "step"
  private[graph] final val keyUnit         = "unit"
  private[graph] final val keyPrototype    = "prototype"
  private[graph] final val keyEditable     = "editable"

  private[graph] final val defaultValue    = 0.0
  private[graph] final val defaultMin      = Double.NegativeInfinity
  private[graph] final val defaultMax      = Double.PositiveInfinity
  private[graph] final val defaultDecimals = 2
  private[graph] final val defaultStep     = 0.1
  private[graph] final val defaultUnit     = ""
  private[graph] final val defaultEditable = true

  final case class Value(w: DoubleField) extends Ex[Double] {
    type Repr[T <: Txn[T]] = IExpr[T, Double]

    override def productPrefix: String = s"DoubleField$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[T]
      val valueOpt  = ctx.getProperty[Ex[Double]](w, keyValue)
      val value0    = valueOpt.fold[Double](defaultValue)(_.expand[T].value)
      new DoubleFieldValueExpandedImpl[T](ws, value0).init()
    }
  }

  final case class Min(w: DoubleField) extends Ex[Double] {
    type Repr[T <: Txn[T]] = IExpr[T, Double]

    override def productPrefix: String = s"DoubleField$$Min" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[T]
    }
  }

  final case class Max(w: DoubleField) extends Ex[Double] {
    type Repr[T <: Txn[T]] = IExpr[T, Double]

    override def productPrefix: String = s"DoubleField$$Max" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[T]
    }
  }

  final case class Step(w: DoubleField) extends Ex[Double] {
    type Repr[T <: Txn[T]] = IExpr[T, Double]

    override def productPrefix: String = s"DoubleField$$Step" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Double]](w, keyStep)
      valueOpt.getOrElse(Const(defaultStep)).expand[T]
    }
  }

  final case class Decimals(w: DoubleField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"DoubleField$$Decimals" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyDecimals)
      valueOpt.getOrElse(Const(defaultDecimals)).expand[T]
    }
  }

  final case class Unit(w: DoubleField) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"DoubleField$$Unit" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyUnit)
      valueOpt.getOrElse(Const(defaultUnit)).expand[T]
    }
  }

  private[graph] def defaultPrototype[T <: Txn[T]](w: DoubleField)(implicit ctx: Context[T], tx: T): Ex[Seq[Double]] = {
    val seq0 = ctx.getProperty[Ex[Double]](w, keyValue).toList
    ExSeq(w.min :: w.max :: seq0: _*)
  }

  final case class Prototype(w: DoubleField) extends Ex[Seq[Double]] {
    type Repr[T <: Txn[T]] = IExpr[T, Seq[Double]]

    override def productPrefix: String = s"DoubleField$$Prototype" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Seq[Double]]](w, keyPrototype)
      valueOpt.getOrElse(defaultPrototype(w)).expand[T]
    }
  }

  final case class Editable(w: DoubleField) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"DoubleField$$Editable" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[T]
    }
  }

  private final case class Impl() extends DoubleField with ComponentImpl { w =>
    override def productPrefix: String = "DoubleField" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): DoubleField.Repr[T] =
      new DoubleFieldExpandedImpl[T](this, tx).initComponent()

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

    def prototype: Ex[Seq[Double]] = Prototype(this)

    def prototype_=(x: Ex[Seq[Double]]): scala.Unit = {
      val b = Graph.builder
      b.putProperty(this, keyPrototype, x)
    }
  }

  trait Repr[T <: Txn[T]] extends View[T] with IControl[T] {
    type C = View.Component

    def doubleField: View.DoubleField

    private[graph] def value: IExpr[T, Double]
  }
}
trait DoubleField extends NumberField[Double] {
//  type C = de.sciss.audiowidgets.ParamField[Double]
  type C = View.Component

  var decimals: Ex[Int]

  type Repr[T <: Txn[T]] = DoubleField.Repr[T]
}