/*
 *  Bang.scala
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
import de.sciss.lucre.expr.graph.{Act, Trig}
import de.sciss.lucre.expr.{Context, IAction, IControl, ITrigger}
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.graph.impl.BangExpandedImpl

object Bang {
  def apply(): Bang = Impl()

  private final case class Impl() extends Bang with ComponentImpl {
    override def productPrefix = "Bang"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      new BangExpandedImpl[T](this).initComponent()
    }
  }
}
trait Bang extends Component with Act with Trig {
  final type C = View.Component // scala.swing.Button

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T] with ITrigger[T] with IAction[T]
}