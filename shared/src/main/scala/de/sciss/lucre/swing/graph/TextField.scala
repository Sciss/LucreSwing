/*
 *  TextField.scala
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
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.graph.impl.TextFieldExpandedImpl
import de.sciss.lucre.{IExpr, Txn}

object TextField {

  def apply(): TextField = Impl()

  def apply(columns: Ex[Int]): TextField = {
    val res = apply()
    res.columns = columns
    res
  }

  private[graph] final val keyText         = "text"
  private[graph] final val keyColumns      = "columns"
  private[graph] final val keyEditable     = "editable"
  private[graph] final val defaultText     = ""
  private[graph] final val defaultColumns  = 0
  private[graph] final val defaultEditable = true

  final case class Text(w: TextField) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"TextField$$Text" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.text
//      import ctx.{cursor, targets}
//      val valueOpt  = ctx.getProperty[Ex[String]](w, keyText)
//      val value0    = valueOpt.fold[String](defaultText)(_.expand[T].value)
//      new TextExpanded[T](ws, value0).init()
    }
  }

  final case class Columns(w: TextField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"TextField$$Columns" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Const(defaultColumns)).expand[T]
    }
  }

  final case class Editable(w: TextField) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"TextField$$Editable" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[T]
    }
  }

  private final case class Impl() extends TextField with ComponentImpl { w =>
    override def productPrefix: String = "TextField" // serialization
    
    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new TextFieldExpandedImpl[T](this, tx).initComponent()

    object text extends Model[String] {
      def apply(): Ex[String] = Text(w)

      def update(value: Ex[String]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyText, value)
      }
    }

    def columns: Ex[Int] = Columns(this)

    def columns_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyColumns, value)
    }

    def editable: Ex[Boolean] = Editable(this)

    def editable_=(value: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyEditable, value)
    }
  }

  trait Repr[T <: Txn[T]] extends View[T] with IControl[T] {
    type C = View.Component

    def textField: View.TextField

    private[graph] def text: IExpr[T, String]
  }
}
trait TextField extends Component {
  type C = View.Component // TextField // scala.swing.TextField

  type Repr[T <: Txn[T]] = TextField.Repr[T] // View.T[T, C] with IControl[T]

  var columns: Ex[Int]

  def text: Model[String]

  var editable: Ex[Boolean]
}
