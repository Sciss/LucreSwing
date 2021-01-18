/*
 *  Slider.scala
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
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.SliderExpandedImpl
import de.sciss.lucre.{IExpr, Txn}

object Slider extends ProductReader[Slider] {
  def apply(): Slider = Impl()

  def apply(min: Ex[Int] = defaultMin,
            max: Ex[Int] = defaultMax,
           ): Slider = {
    val res = Slider()
    if (min != Const(defaultMin)) res.min = min
    if (max != Const(defaultMax)) res.max = max
    res
  }

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Slider = {
    require (arity == 0 && adj == 0)
    Slider()
  }

  private[graph] final val defaultValue  =  50
  private[graph] final val defaultMin    =   0
  private[graph] final val defaultMax    = 100
  
  private[graph] final val keyValue      = "value"
  private[graph] final val keyMin        = "min"
  private[graph] final val keyMax        = "max"

  object Value extends ProductReader[Value] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Value = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Slider]()
      new Value(_w)
    }
  }
  final case class Value(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.value
    }
  }

  object Min extends ProductReader[Min] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Min = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Slider]()
      new Min(_w)
    }
  }
  final case class Min(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Min" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[T]
    }
  }

  object Max extends ProductReader[Max] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Max = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Slider]()
      new Max(_w)
    }
  }
  final case class Max(w: Slider) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Slider$$Max" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[T]
    }
  }

  private final case class Impl() extends Slider with ComponentImpl { w =>
    override def productPrefix = "Slider"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Slider.Repr[T] =
      new SliderExpandedImpl[T](this, tx).initComponent()

    def min: Ex[Int] = Min(this)

    def min_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMin, x)
    }

    def max: Ex[Int] = Max(this)

    def max_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMax, x)
    }

    object value extends Model[Int] {
      def apply(): Ex[Int] = Value(w)

      def update(x: Ex[Int]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValue, x)
      }
    }
  }

  trait Repr[T <: Txn[T]] extends View[T] with IControl[T] {
    type C = View.Component

    def slider: View.Slider

    private[graph] def value: IExpr[T, Int]
  }
}
trait Slider extends Component {
  type C = View.Component // scala.swing.Slider

  type Repr[T <: Txn[T]] = Slider.Repr[T]

  var min   : Ex[Int]
  var max   : Ex[Int]

  def value : Model[Int]
}