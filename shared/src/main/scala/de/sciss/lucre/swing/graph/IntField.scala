/*
 *  IntField.scala
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

import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, ExSeq, Graph, IControl, Model}
import de.sciss.lucre.swing.graph.impl.{ComponentImpl, IntFieldExpandedImpl}
import de.sciss.lucre.swing.View
import de.sciss.lucre.{IExpr, Txn}

object IntField extends ProductReader[IntField] {
  def apply(): IntField = Impl()

  def apply(min : Ex[Int]     = defaultMin,
            max : Ex[Int]     = defaultMax,
            step: Ex[Int]     = defaultStep,
            unit: Ex[String]  = defaultUnit,
           ): IntField = {
    val res = IntField()
    if (min  != Const(defaultMin  )) res.min  = min
    if (max  != Const(defaultMax  )) res.max  = max
    if (step != Const(defaultStep )) res.step = step
    if (unit != Const(defaultUnit )) res.unit = unit
    res
  }

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): IntField = {
    require (arity == 0 && adj == 0)
    IntField()
  }

  private[graph] final val keyValue        = "value"
  private[graph] final val keyMin          = "min"
  private[graph] final val keyMax          = "max"
  private[graph] final val keyStep         = "step"
  private[graph] final val keyUnit         = "unit"
  private[graph] final val keyPrototype    = "prototype"
  private[graph] final val keyEditable     = "editable"

  private[graph] final val defaultValue    = 0
  private[graph] final val defaultMin      = Int.MinValue
  private[graph] final val defaultMax      = Int.MaxValue
  private[graph] final val defaultStep     = 1
  private[graph] final val defaultUnit     = ""
  private[graph] final val defaultEditable = true

  object Value extends ProductReader[Value] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Value = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Value(_w)
    }
  }
  final case class Value(w: IntField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"IntField$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.value
    }
  }

  object Min extends ProductReader[Min] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Min = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Min(_w)
    }
  }
  final case class Min(w: IntField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"IntField$$Min" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[T]
    }
  }

  object Max extends ProductReader[Max] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Max = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Max(_w)
    }
  }
  final case class Max(w: IntField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"IntField$$Max" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[T]
    }
  }

  object Step extends ProductReader[Step] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Step = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Step(_w)
    }
  }
  final case class Step(w: IntField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"IntField$$Step" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyStep)
      valueOpt.getOrElse(Const(defaultStep)).expand[T]
    }
  }

  object Unit extends ProductReader[Unit] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Unit = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Unit(_w)
    }
  }
  final case class Unit(w: IntField) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"IntField$$Unit" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyUnit)
      valueOpt.getOrElse(Const(defaultUnit)).expand[T]
    }
  }

  private[graph] def defaultPrototype[T <: Txn[T]](w: IntField)(implicit ctx: Context[T], tx: T): Ex[Seq[Int]] = {
    val seq0 = ctx.getProperty[Ex[Int]](w, keyValue).toList
    ExSeq(w.min :: w.max :: seq0: _*)
  }

  object Prototype extends ProductReader[Prototype] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Prototype = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Prototype(_w)
    }
  }
  final case class Prototype(w: IntField) extends Ex[Seq[Int]] {
    type Repr[T <: Txn[T]] = IExpr[T, Seq[Int]]

    override def productPrefix: String = s"IntField$$Prototype" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Seq[Int]]](w, keyPrototype)
      valueOpt.getOrElse(defaultPrototype(w)).expand[T]
    }
  }

  object Editable extends ProductReader[Editable] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Editable = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[IntField]()
      new Editable(_w)
    }
  }
  final case class Editable(w: IntField) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"IntField$$Editable" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[T]
    }
  }

  private final case class Impl() extends IntField with ComponentImpl { w =>
    override def productPrefix: String = "IntField" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): IntField.Repr[T] =
      new IntFieldExpandedImpl[T](this, tx).initComponent()

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

  trait Repr[T <: Txn[T]] extends View[T] with IControl[T] {
    type C = View.Component

    def intField: View.IntField

    private[graph] def value: IExpr[T, Int]
  }
}
trait IntField extends NumberField[Int] {
//  type C = de.sciss.audiowidgets.ParamField[Int]
  type C = View.Component

  type Repr[T <: Txn[T]] = IntField.Repr[T] // View.T[T, C] with IControl[T]
}