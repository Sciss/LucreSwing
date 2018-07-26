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

package de.sciss.lucre.swing
package graph

import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object FlowPanel {
  def apply(contents: Widget*): FlowPanel = Impl(contents)

//  def mk(configure: FlowPanel => Unit): FlowPanel = {
//    val w = apply()
//    configure(w)
//    w
//  }

  private final class Expanded[S <: Sys[S]](protected val w: FlowPanel) extends View[S]
    with ComponentHolder[scala.swing.FlowPanel] with ComponentExpandedImpl[S] {

    type C = scala.swing.FlowPanel

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val contentsV = w.contents.map(_.expand[S])
      deferTx {
        val vec = contentsV.map(_.component)
        component = new scala.swing.FlowPanel(vec: _*)
      }
      super.init()
    }

//    def dispose()(implicit tx: S#Tx): Unit = super.dispose()
  }

  private final case class Impl(contents: Seq[Widget]) extends FlowPanel with ComponentImpl {
    override def productPrefix = "FlowPanel" // s"FlowPanel$$Impl" // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      new Expanded[S](this).init()
    }
  }
}
trait FlowPanel extends Panel {
  type C = scala.swing.FlowPanel
}
