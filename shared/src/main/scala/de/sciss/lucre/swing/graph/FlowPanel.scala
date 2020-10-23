/*
 *  FlowPanel.scala
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

package de.sciss.lucre.swing
package graph

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.graph.impl.PanelImpl
import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.swing.graph.impl.FlowPanelExpandedImpl

//import scala.swing.{FlowPanel => Peer}

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  final case class HGap(w: FlowPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"FlowPanel$$HGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[T]
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
