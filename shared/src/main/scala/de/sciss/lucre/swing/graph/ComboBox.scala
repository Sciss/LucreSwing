/*
 *  ComboBox.scala
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

import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.graph.impl.ComboBoxExpandedPlatform
import de.sciss.lucre.swing.View
import de.sciss.lucre.{IExpr, Txn}

object ComboBox {

  def apply[A](items: Ex[Seq[A]]): ComboBox[A] = Impl(items)

  private[graph] final val keyIndex          = "index"
  private[graph] final val keyValueOption    = "valueOption"

  final case class Index[A](w: ComboBox[A]) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"ComboBox$$Index" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.index
    }
  }

  final case class ValueOption[A](w: ComboBox[A]) extends Ex[Option[A]] {
    type Repr[T <: Txn[T]] = IExpr[T, Option[A]]

    override def productPrefix: String = s"ComboBox$$ValueOption" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws = w.expand[T]
      ws.valueOption
    }
  }

  private final case class Impl[A](items: Ex[Seq[A]]) extends ComboBox[A] with ComponentImpl { w =>
    override def productPrefix = "ComboBox"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): ComboBox.Repr[T, A] =
      new ComboBoxExpandedPlatform[T, A](this, tx).initComponent()

    object index extends Model[Int] {
      def apply(): Ex[Int] = Index(w)

      def update(value: Ex[Int]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyIndex, value)
      }
    }

    object valueOption extends Model[Option[A]] {
      def apply(): Ex[Option[A]] = ValueOption(w)

      def update(value: Ex[Option[A]]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValueOption, value)
      }
    }
  }

  trait Repr[T <: Txn[T], A] extends View[T] with IControl[T] {
    type C = View.Component

    def comboBox: View.ComboBox[A]

    private[graph] def index: IExpr[T, Int]

    private[graph] def valueOption: IExpr[T, Option[A]]
  }
}
trait ComboBox[A] extends Component {
//  type C = de.sciss.swingplus.ComboBox[A]
  type C = View.Component

  type Repr[T <: Txn[T]] = ComboBox.Repr[T, A]

  def items: Ex[Seq[A]]

  /** Index of selected item or `-1` */
  def index: Model[Int]

  /** Some selected item or `None` */
  def valueOption: Model[Option[A]]
}