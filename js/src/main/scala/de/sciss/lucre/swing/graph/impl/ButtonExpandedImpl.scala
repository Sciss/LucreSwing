package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import com.raquo.laminar.api.L.{Button => Peer, _}

final class ButtonExpandedImpl[T <: Txn[T]](protected val peer: Button) extends View[T]
  with ComponentHolder[Peer] with ComponentExpandedImpl[T] {

  type C = View.Button

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val text      = peer.text.expand[T]
    val text0     = text.value
    val text1     = if (text0.isEmpty) null else text0
    deferTx {
      val c = button(
        text1,
        cls := "lucre-button",
      )
      component = c
    }

    initProperty(Component.keyEnabled, Component.defaultEnabled)(p => component.amend(disabled := !p))
    super.initComponent()
  }
}
