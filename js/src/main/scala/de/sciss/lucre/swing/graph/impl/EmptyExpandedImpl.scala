/*
 *  EmptyExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.IControl
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder

final class EmptyExpandedImpl[T <: Txn[T]] extends View[T] with IControl[T]
  with ComponentHolder[Span] {

  type C = View.Component

  def initControl()(implicit tx: T): Unit = ()

  def initComponent()(implicit tx: T): this.type = {
    deferTx {
      component = span(
        cls := "lucre-empty"
      )
    }
    this
  }

  def dispose()(implicit tx: T): Unit = ()
}
