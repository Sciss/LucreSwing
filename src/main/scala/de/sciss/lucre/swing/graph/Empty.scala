/*
 *  Empty.scala
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

import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.Dimension

object Empty {
  def apply(): Empty = Impl() // instance

//  private[graph] val instance: Empty = Impl()

  private final class Expanded[S <: Sys[S]] extends View[S] with IControl[S]
    with ComponentHolder[scala.swing.Component] {

    type C = scala.swing.Component

    def initControl()(implicit tx: S#Tx): Unit = ()

    def initComponent()(implicit tx: S#Tx): this.type = {
      deferTx {
        component = scala.swing.Swing.RigidBox(new Dimension(0, 0))
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()
  }

  private final case class Impl() extends Empty {
    override def productPrefix = "Empty"  // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S].initComponent()
  }
}
/** This is a placeholder widget that can be eliminated in other places of the API,
  * for example `BorderPanel` contents.
  */
trait Empty extends Widget {
  type C = scala.swing.Component

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]
}