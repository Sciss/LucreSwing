/*
 *  CheckBox.scala
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
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{CheckBoxExpandedImpl, ComponentImpl}
import de.sciss.lucre.{IExpr, Txn}

object CheckBox extends ProductReader[CheckBox] {
  def apply(text: Ex[String] = ""): CheckBox = Impl(text)

  private[graph] final val defaultSelected = false
  private[graph] final val keySelected     = "selected"

  object Selected extends ProductReader[Selected] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Selected = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[CheckBox]()
      new Selected(_w)
    }
  }
  final case class Selected(w: CheckBox) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"CheckBox$$Selected" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.selected
    }
  }

  private final case class Impl(text0: Ex[String]) extends CheckBox with ComponentImpl { w =>
    override def productPrefix = "CheckBox"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): CheckBox.Repr[T] =
      new CheckBoxExpandedImpl[T](this, tx).initComponent()

    object selected extends Model[Boolean] {
      def apply(): Ex[Boolean] = Selected(w)

      def update(value: Ex[Boolean]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keySelected, value)
      }
    }

    def text: Ex[String] = text0
  }

  trait Repr[T <: Txn[T]] extends View[T] with IControl[T] {
    type C = View.Component

    def checkBox: View.CheckBox

    private[graph] def selected: IExpr[T, Boolean]
  }

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): CheckBox = {
    require (arity == 1 && adj == 0)
    val _text = in.readEx[String]()
    CheckBox(_text)
  }
}
trait CheckBox extends Component {
  type C = View.Component

  type Repr[T <: Txn[T]] = CheckBox.Repr[T] // View.T[T, C] with IControl[T]

  def text: Ex[String]

  def selected: Model[Boolean]
}