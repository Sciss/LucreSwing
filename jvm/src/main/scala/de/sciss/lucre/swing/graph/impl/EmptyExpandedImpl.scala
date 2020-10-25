/*
 *  EmptyExpandedImpl.scala
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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.IControl
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.Dimension

final class EmptyExpandedImpl[T <: Txn[T]] extends View[T] with IControl[T]
  with ComponentHolder[scala.swing.Component] {

  type C = View.Component

  def initControl()(implicit tx: T): Unit = ()

  def initComponent()(implicit tx: T): this.type = {
    deferTx {
      component = scala.swing.Swing.RigidBox(new Dimension(0, 0))
    }
    this
  }

  def dispose()(implicit tx: T): Unit = ()
}
