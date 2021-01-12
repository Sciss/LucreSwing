/*
 *  Separator.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
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
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.SeparatorExpandedImpl

object Separator {
  def apply(): Separator = Impl()

  private final case class Impl() extends Separator {
    override def productPrefix = "Separator"  // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new SeparatorExpandedImpl[T].initComponent()
  }
}
trait Separator extends Widget {
  type C = View.Component // scala.swing.Separator

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]
}