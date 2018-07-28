/*
 *  Empty.scala
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
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.Dimension

object Empty {
  def apply(): Empty = instance

  private[graph] val instance: Empty = Impl()

  private final class Expanded[S <: Sys[S]] extends View[S]
    with ComponentHolder[scala.swing.Component] {

    type C = scala.swing.Component

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        component = scala.swing.Swing.RigidBox(new Dimension(0, 0))
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()
  }

  private final case class Impl() extends Empty {
    override def productPrefix = "Empty"  // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      new Expanded[S].init()
    }
  }
}
/** This is a placeholder widget that can be eliminated in other places of the API,
  * for example `BorderPanel` contents.
  */
trait Empty extends Widget {
  type C = scala.swing.Component
}