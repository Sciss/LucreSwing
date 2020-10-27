/*
 *  ProgressBar.scala
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

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl}
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.graph.impl.ProgressBarExpandedImpl
import de.sciss.lucre.swing.View
import de.sciss.lucre.{IExpr, Txn}

object ProgressBar {
  def apply(): ProgressBar = Impl()

  private[graph] final val keyValue            = "value"
  private[graph] final val keyMin              = "min"
  private[graph] final val keyMax              = "max"
  private[graph] final val keyLabel            = "label"
  private[graph] final val keyLabelPainted     = "labelPainted"

  private[graph] final val defaultMin          =   0
  private[graph] final val defaultMax          = 100
  private[graph] final val defaultLabel        = ""
  private[graph] final val defaultLabelPainted = false

  final case class Value(w: ProgressBar) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"ProgressBar$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue)
      valueOpt.orElse(ctx.getProperty[Ex[Int]](w, keyMin)).getOrElse(Const(defaultMin)).expand[T]
    }
  }

  final case class Min(w: ProgressBar) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"ProgressBar$$Min" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[T]
    }
  }

  final case class Max(w: ProgressBar) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"ProgressBar$$Max" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[T]
    }
  }

  final case class Label(w: ProgressBar) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"ProgressBar$$String" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyLabel)
      valueOpt.getOrElse(Const(defaultLabel)).expand[T]
    }
  }

  final case class LabelPainted(w: ProgressBar) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"ProgressBar$$LabelPainted" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyLabelPainted)
      valueOpt.getOrElse(Const(defaultLabelPainted)).expand[T]
    }
  }

  private final case class Impl() extends ProgressBar with ComponentImpl {
    override def productPrefix = "ProgressBar"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new ProgressBarExpandedImpl[T](this).initComponent()

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

    def value: Ex[Int] = Value(this)

    def value_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyValue, x)
    }

    def label: Ex[String] = ProgressBar.Label(this)

    def label_=(x: Ex[String]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyLabel, x)
    }

    def labelPainted: Ex[Boolean] = LabelPainted(this)

    def labelPainted_=(x: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyLabelPainted, x)
    }
  }
}
trait ProgressBar extends Component {
  type C = View.Component // scala.swing.ProgressBar

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  var min         : Ex[Int]
  var max         : Ex[Int]
  var value       : Ex[Int]

  var label       : Ex[String]
  var labelPainted: Ex[Boolean]
}