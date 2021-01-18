/*
 *  FlowPanel.scala
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
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.graph.impl.PanelImpl
import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.swing.graph.impl.FlowPanelExpandedImpl

object FlowPanel extends ProductReader[FlowPanel] {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): FlowPanel = {
    require (arity == 1 && adj == 0)
    val _contents = in.readVec(in.readProductT[Widget]())
    FlowPanel(_contents: _*)
  }

  object HGap extends ProductReader[HGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): HGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[FlowPanel]()
      new HGap(_w)
    }
  }
  final case class HGap(w: FlowPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"FlowPanel$$HGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[T]
    }
  }

  object VGap extends ProductReader[VGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): VGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[FlowPanel]()
      new VGap(_w)
    }
  }
  final case class VGap(w: FlowPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"FlowPanel$$VGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[T]
    }
  }

  object Align extends ProductReader[Align] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Align = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[FlowPanel]()
      new Align(_w)
    }
  }
  final case class Align(w: Component) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"FlowPanel$$Align" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyAlign)
      valueOpt.fold(Const(defaultAlign).expand[T])(_.expand[T])
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel with PanelImpl {
    override def productPrefix = "FlowPanel" // s"FlowPanel$$Impl" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new FlowPanelExpandedImpl[T](this).initComponent()

    def hGap: Ex[Int] = HGap(this)

    def hGap_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyHGap, x)
    }

    def vGap: Ex[Int] = VGap(this)

    def vGap_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyVGap, x)
    }

    def align: Ex[Int] = FlowPanel.Align(this)

    def align_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyAlign, value)
    }
  }

  private[graph] final val keyHGap               = "hGap"
  private[graph] final val keyVGap               = "vGap"
  private[graph] final val keyAlign              = "align"
  private[graph] final val defaultHGap           = 4
  private[graph] final val defaultVGap           = 2
  private[graph]       def defaultAlign: Int     = graph.Align.Center
}
trait FlowPanel extends Panel {
  type C = View.Component // Peer

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components (if they get wrapped). The default value is 2. */
  var vGap: Ex[Int]

  /** Line alignment. One of `Align.Left`, `Align.Center` (default), `Align.Trailing` */
  var align: Ex[Int]
}
