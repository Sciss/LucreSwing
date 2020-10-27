/*
 *  SeparatorExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.IControl
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder

final class SeparatorExpandedImpl[T <: Txn[T]] extends View[T] with IControl[T]
  with ComponentHolder[View.Component] {

  type C = View.Component // scala.swing.Separator

  def initControl()(implicit tx: T): Unit = ()

  def initComponent()(implicit tx: T): this.type = {
    deferTx {
      component = hr(
        cls := "lucre-separator"
      )
    }
    this
  }

  def dispose()(implicit tx: T): Unit = ()
}