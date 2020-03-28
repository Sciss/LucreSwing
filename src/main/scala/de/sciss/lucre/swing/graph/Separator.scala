/*
 *  Separator.scala
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

object Separator {
  def apply(): Separator = Impl()

  private final class Expanded[S <: Sys[S]] extends View[S] with IControl[S]
    with ComponentHolder[scala.swing.Separator] {

    type C = scala.swing.Separator

    def initControl()(implicit tx: S#Tx): Unit = ()

    def initComponent()(implicit tx: S#Tx): this.type = {
      deferTx {
        component = new scala.swing.Separator
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()
  }

  private final case class Impl() extends Separator {
    override def productPrefix = "Separator"  // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S].initComponent()
  }
}
trait Separator extends Widget {
  type C = scala.swing.Separator

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]
}