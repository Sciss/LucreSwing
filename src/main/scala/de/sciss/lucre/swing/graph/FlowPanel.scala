/*
 *  FlowPanel.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{PanelExpandedImpl, PanelImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{Graph, View, deferTx, graph}

import scala.swing.{FlowPanel => Peer}

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  private final class Expanded[S <: Sys[S]](protected val w: FlowPanel) extends View[S]
    with ComponentHolder[Peer] with PanelExpandedImpl[S] {

    type C = Peer

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val hGap      = ctx.getProperty[Ex[Int    ]](w, keyHGap    ).fold(defaultHGap    )(_.expand[S].value)
      val vGap      = ctx.getProperty[Ex[Int    ]](w, keyVGap    ).fold(defaultVGap    )(_.expand[S].value)
      val align     = ctx.getProperty[Ex[Int    ]](w, keyAlign   ).fold(defaultAlign   )(_.expand[S].value)
      val contentsV = w.contents.map(_.expand[S])
      deferTx {
        val alignSwing = align match {
          case graph.Align.Left     => Peer.Alignment.Left
          case graph.Align.Right    => Peer.Alignment.Right
          case graph.Align.Trailing => Peer.Alignment.Trailing
          case graph.Align.Leading  => Peer.Alignment.Leading
          case _                    => Peer.Alignment.Center
        }
        val vec = contentsV.map(_.component)
        val c = new Peer(alignSwing)(vec: _*)
        c.hGap = hGap
        c.vGap = vGap
        component = c
      }
      super.init()
    }
  }

  final case class HGap(w: FlowPanel) extends Ex[Int] {
    override def productPrefix: String = s"FlowPanel$$HGap" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Constant(defaultHGap)).expand[S]
    }
  }

  final case class VGap(w: FlowPanel) extends Ex[Int] {
    override def productPrefix: String = s"FlowPanel$$VGap" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Constant(defaultVGap)).expand[S]
    }
  }

  final case class Align(w: Component) extends Ex[Int] {
    override def productPrefix: String = s"FlowPanel$$Align" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyAlign)
      valueOpt.fold(Constant(defaultAlign).expand[S])(_.expand[S])
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel with PanelImpl {
    override def productPrefix = "FlowPanel" // s"FlowPanel$$Impl" // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

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

  private final val keyHGap               = "hGap"
  private final val keyVGap               = "vGap"
  private final val keyAlign              = "align"
  private final val defaultHGap           = 4
  private final val defaultVGap           = 2
  private       def defaultAlign: Int     = graph.Align.Center
}
trait FlowPanel extends Panel {
  type C = Peer

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components (if they get wrapped). The default value is 2. */
  var vGap: Ex[Int]

  /** Line alignment. One of `Align.Left`, `Align.Center` (default), `Align.Trailing` */
  var align: Ex[Int]
}
