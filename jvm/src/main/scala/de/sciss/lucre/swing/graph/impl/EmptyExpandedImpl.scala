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
