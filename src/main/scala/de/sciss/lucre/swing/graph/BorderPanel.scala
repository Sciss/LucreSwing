/*
 *  BorderPanel.scala
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

package de.sciss.lucre.swing
package graph

import java.awt.BorderLayout

import de.sciss.lucre.expr.Ex
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object BorderPanel {
  def apply(north : Widget = Empty(),
            south : Widget = Empty(),
            west  : Widget = Empty(),
            east  : Widget = Empty(),
            center: Widget = Empty()
           ): BorderPanel =
    Impl(north = north, south = south, west = west, east = east, center = center)

  private final class Expanded[S <: Sys[S]](protected val w: BorderPanel) extends View[S]
    with ComponentHolder[scala.swing.BorderPanel] with ComponentExpandedImpl[S] {

    type C = scala.swing.BorderPanel

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val north : View[S] = if (w.north   != Empty.instance) w.north  .expand[S] else null
      val south : View[S] = if (w.south   != Empty.instance) w.south  .expand[S] else null
      val west  : View[S] = if (w.west    != Empty.instance) w.west   .expand[S] else null
      val east  : View[S] = if (w.east    != Empty.instance) w.east   .expand[S] else null
      val center: View[S] = if (w.center  != Empty.instance) w.center .expand[S] else null
      deferTx {
        val p     = new scala.swing.BorderPanel
        val peer  = p.peer
        if (north   != null) peer.add(north .component.peer, BorderLayout.NORTH )
        if (south   != null) peer.add(south .component.peer, BorderLayout.SOUTH )
        if (west    != null) peer.add(west  .component.peer, BorderLayout.WEST  )
        if (east    != null) peer.add(east  .component.peer, BorderLayout.EAST  )
        if (center  != null) peer.add(center.component.peer, BorderLayout.CENTER)
        component = p
      }
      super.init()
    }
  }

  private final case class Impl(north: Widget, south: Widget, west: Widget, east: Widget,
                                center: Widget) extends BorderPanel with ComponentImpl {
    override def productPrefix = "BorderPanel" // s"BorderPanel$$Impl" // serialization

    def contents: Seq[Widget] = {
      var res = List.empty[Widget]
      if (center  != Empty.instance) res ::= center
      if (east    != Empty.instance) res ::= east
      if (west    != Empty.instance) res ::= west
      if (south   != Empty.instance) res ::= south
      if (north   != Empty.instance) res ::= north
      res
    }

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()
  }
}
trait BorderPanel extends Panel {
  type C = scala.swing.BorderPanel

  def north : Widget
  def south : Widget
  def west  : Widget
  def east  : Widget
  def center: Widget
}
