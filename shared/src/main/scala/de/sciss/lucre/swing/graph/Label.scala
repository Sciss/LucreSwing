/*
 *  Label.scala
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

import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.swing.graph.impl.{ComponentImpl, LabelExpandedImpl}
import de.sciss.lucre.expr.{Context, Graph, IControl}
import de.sciss.lucre.{IExpr, Txn}

object Label extends ProductReader[Label] {
  def apply(text: Ex[String]): Label = Impl(text)

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Label = {
    require (arity == 1 && adj == 0)
    val _text = in.readEx[String]()
    Label(_text)
  }

  object HAlign extends ProductReader[HAlign] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): HAlign = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Component]()
      new HAlign(_w)
    }
  }
  final case class HAlign(w: Component) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Label$$HAlign" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHAlign)
      valueOpt.fold(Const(defaultHAlign).expand[T])(_.expand[T])
    }
  }

  object VAlign extends ProductReader[VAlign] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): VAlign = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Component]()
      new VAlign(_w)
    }
  }
  final case class VAlign(w: Component) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"Label$$VAlign" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVAlign)
      valueOpt.fold(Const(defaultVAlign).expand[T])(_.expand[T])
    }
  }

  private final case class Impl(text0: Ex[String]) extends Label with ComponentImpl {
    override def productPrefix: String = "Label" // serialization

//    type C = View.Component

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new LabelExpandedImpl[T](this).initComponent()

    def text: Ex[String] = text0

    def hAlign: Ex[Int] = Label.HAlign(this)

    def hAlign_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyHAlign, value)
    }

    def vAlign: Ex[Int] = Label.VAlign(this)

    def vAlign_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyVAlign, value)
    }
  }

  private[graph] final val keyHAlign    = "hAlign"
  private[graph] final val keyVAlign    = "vAlign"

  private[graph] def defaultHAlign: Int = Align.Leading
  private[graph] def defaultVAlign: Int = Align.Center
}
trait Label extends Component {
  type C = View.Component

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  /** The label's text */
  def text: Ex[String]

  /** Horizontal alignment:
    * The alignment must be one of `Align.Left`, `Align.Center`, `Align.Right`, `Align.Leading`, `Align.Trailing`.
    * Setting an invalid value makes the component default aligned (`Leading`).
    */
  var hAlign: Ex[Int]

  /** Vertical alignment:
    * The alignment must be one of `Align.Top`, `Align.Center`, `Align.Bottom`.
    * Setting an invalid value makes the component default aligned (`Center`).
    */
  var vAlign: Ex[Int]
}