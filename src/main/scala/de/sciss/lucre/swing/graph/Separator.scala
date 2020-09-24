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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.impl.ComponentHolder

object Separator {
  def apply(): Separator = Impl()

  private final class Expanded[T <: Txn[T]] extends View[T] with IControl[T]
    with ComponentHolder[scala.swing.Separator] {

    type C = scala.swing.Separator

    def initControl()(implicit tx: T): Unit = ()

    def initComponent()(implicit tx: T): this.type = {
      deferTx {
        component = new scala.swing.Separator
      }
      this
    }

    def dispose()(implicit tx: T): Unit = ()
  }

  private final case class Impl() extends Separator {
    override def productPrefix = "Separator"  // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T].initComponent()
  }
}
trait Separator extends Widget {
  type C = scala.swing.Separator

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]
}