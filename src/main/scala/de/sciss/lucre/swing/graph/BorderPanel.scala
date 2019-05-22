/*
 *  BorderPanel.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import java.awt.BorderLayout

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.impl.{PanelExpandedImpl, PanelImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{Graph, View}

object BorderPanel {
  def apply(north : Widget = Empty(),
            south : Widget = Empty(),
            west  : Widget = Empty(),
            east  : Widget = Empty(),
            center: Widget = Empty()
           ): BorderPanel =
    Impl(north = north, south = south, west = west, east = east, center = center)

  private final class Expanded[S <: Sys[S]](protected val peer: BorderPanel)
    extends ComponentHolder[scala.swing.BorderPanel] with PanelExpandedImpl[S] {

    type C = scala.swing.BorderPanel

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[S].value)
      val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[S].value)
      val north : View[S] = if (peer.north .isInstanceOf[Empty]) peer.north  .expand[S] else null
      val south : View[S] = if (peer.south .isInstanceOf[Empty]) peer.south  .expand[S] else null
      val west  : View[S] = if (peer.west  .isInstanceOf[Empty]) peer.west   .expand[S] else null
      val east  : View[S] = if (peer.east  .isInstanceOf[Empty]) peer.east   .expand[S] else null
      val center: View[S] = if (peer.center.isInstanceOf[Empty]) peer.center .expand[S] else null
      deferTx {
        val p     = new scala.swing.BorderPanel
        val lay   = p.layoutManager
        lay.setHgap(hGap)
        lay.setVgap(vGap)
        val peer  = p.peer
        if (north   != null) peer.add(north .component.peer, BorderLayout.NORTH )
        if (south   != null) peer.add(south .component.peer, BorderLayout.SOUTH )
        if (west    != null) peer.add(west  .component.peer, BorderLayout.WEST  )
        if (east    != null) peer.add(east  .component.peer, BorderLayout.EAST  )
        if (center  != null) peer.add(center.component.peer, BorderLayout.CENTER)
        component = p
      }
      super.initComponent()
    }
  }

  final case class HGap(w: BorderPanel) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"BorderPanel$$HGap" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[S]
    }
  }

  final case class VGap(w: BorderPanel) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"BorderPanel$$VGap" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[S]
    }
  }

  private final case class Impl(north: Widget, south: Widget, west: Widget, east: Widget,
                                center: Widget) extends BorderPanel with PanelImpl {
    override def productPrefix = "BorderPanel" // s"BorderPanel$$Impl" // serialization

    def contents: Seq[Widget] = {
      var res = List.empty[Widget]
      if (center.isInstanceOf[Empty]) res ::= center
      if (east  .isInstanceOf[Empty]) res ::= east
      if (west  .isInstanceOf[Empty]) res ::= west
      if (south .isInstanceOf[Empty]) res ::= south
      if (north .isInstanceOf[Empty]) res ::= north
      res
    }

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

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()
  }

  private final val keyHGap     = "hGap"
  private final val keyVGap     = "vGap"
  private final val defaultHGap = 4
  private final val defaultVGap = 2
}
trait BorderPanel extends Panel {
  type C = scala.swing.BorderPanel

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  def north : Widget
  def south : Widget
  def west  : Widget
  def east  : Widget
  def center: Widget

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components. The default value is 2. */
  var vGap: Ex[Int]
}
