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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.EmptyExpandedImpl

object Empty {
  def apply(): Empty = Impl() // instance

//  private[graph] val instance: Empty = Impl()

  private final case class Impl() extends Empty {
    override def productPrefix = "Empty"  // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new EmptyExpandedImpl[T].initComponent()
  }
}
/** This is a placeholder widget that can be eliminated in other places of the API,
  * for example `BorderPanel` contents.
  */
trait Empty extends Widget {
  type C = View.Component

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]
}