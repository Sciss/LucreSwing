/*
 *  FlowPanel.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import java.awt.FlowLayout

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.impl.{PanelExpandedImpl, PanelImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{Graph, View, graph}

import scala.swing.{FlowPanel => Peer}

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

  private final class Expanded[S <: Sys[S]](protected val peer: FlowPanel) extends View[S]
    with ComponentHolder[Peer] with PanelExpandedImpl[S] {

    type C = Peer

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val hGap      = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[S].value)
      val vGap      = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[S].value)
      val align     = ctx.getProperty[Ex[Int    ]](peer, keyAlign   ).fold(defaultAlign   )(_.expand[S].value)
      val contentsV = peer.contents.map(_.expand[S])
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
        c.peer.getLayout match {
          case fl: FlowLayout => fl.setAlignOnBaseline(true)
          case _ =>
        }
        component = c
      }
      super.initComponent()
    }
  }

  final case class HGap(w: FlowPanel) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"FlowPanel$$HGap" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[S]
    }
  }

  final case class VGap(w: FlowPanel) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"FlowPanel$$VGap" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[S]
    }
  }

  final case class Align(w: Component) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"FlowPanel$$Align" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyAlign)
      valueOpt.fold(Const(defaultAlign).expand[S])(_.expand[S])
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel with PanelImpl {
    override def productPrefix = "FlowPanel" // s"FlowPanel$$Impl" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

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

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components (if they get wrapped). The default value is 2. */
  var vGap: Ex[Int]

  /** Line alignment. One of `Align.Left`, `Align.Center` (default), `Align.Trailing` */
  var align: Ex[Int]
}
