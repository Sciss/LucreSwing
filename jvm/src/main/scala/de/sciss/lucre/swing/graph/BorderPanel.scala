/*
 *  BorderPanel.scala
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

import java.awt.BorderLayout

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.impl.{PanelExpandedImpl, PanelImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{Graph, View}
import de.sciss.lucre.{IExpr, Txn}

object BorderPanel {
  def apply(north : Widget = Empty(),
            south : Widget = Empty(),
            west  : Widget = Empty(),
            east  : Widget = Empty(),
            center: Widget = Empty()
           ): BorderPanel =
    Impl(north = north, south = south, west = west, east = east, center = center)

  private final class Expanded[T <: Txn[T]](protected val peer: BorderPanel)
    extends ComponentHolder[scala.swing.BorderPanel] with PanelExpandedImpl[T] {

    type C = scala.swing.BorderPanel

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
      val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
      val north : View[T] = if (peer.north .isInstanceOf[Empty]) null else peer.north  .expand[T]
      val south : View[T] = if (peer.south .isInstanceOf[Empty]) null else peer.south  .expand[T]
      val west  : View[T] = if (peer.west  .isInstanceOf[Empty]) null else peer.west   .expand[T]
      val east  : View[T] = if (peer.east  .isInstanceOf[Empty]) null else peer.east   .expand[T]
      val center: View[T] = if (peer.center.isInstanceOf[Empty]) null else peer.center .expand[T]
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
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"BorderPanel$$HGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[T]
    }
  }

  final case class VGap(w: BorderPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"BorderPanel$$VGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[T]
    }
  }

  private final case class Impl(north: Widget, south: Widget, west: Widget, east: Widget,
                                center: Widget) extends BorderPanel with PanelImpl {
    override def productPrefix = "BorderPanel" // s"BorderPanel$$Impl" // serialization

    def contents: Seq[Widget] = {
      var res = List.empty[Widget]
      if (!center.isInstanceOf[Empty]) res ::= center
      if (!east  .isInstanceOf[Empty]) res ::= east
      if (!west  .isInstanceOf[Empty]) res ::= west
      if (!south .isInstanceOf[Empty]) res ::= south
      if (!north .isInstanceOf[Empty]) res ::= north
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

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()
  }

  private final val keyHGap     = "hGap"
  private final val keyVGap     = "vGap"
  private final val defaultHGap = 4
  private final val defaultVGap = 2
}
trait BorderPanel extends Panel {
  type C = scala.swing.BorderPanel

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

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
