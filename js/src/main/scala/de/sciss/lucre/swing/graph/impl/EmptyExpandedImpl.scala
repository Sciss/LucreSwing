package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.IControl
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder

import com.raquo.laminar.api.L.{Label => Peer, _}

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
