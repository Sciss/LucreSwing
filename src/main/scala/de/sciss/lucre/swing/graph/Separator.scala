/*
 *  Separator.scala
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

import de.sciss.lucre.expr.Ex
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.ComponentHolder

object Separator {
  def apply(): Separator = Impl()

  private final class Expanded[S <: Sys[S]] extends View[S]
    with ComponentHolder[scala.swing.Separator] {

    type C = scala.swing.Separator

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        component = new scala.swing.Separator
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = ()
  }

  private final case class Impl() extends Separator {
    override def productPrefix = "Separator"  // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S].init()
  }
}
trait Separator extends Widget {
  type C = scala.swing.Separator
}