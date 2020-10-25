/*
 *  ButtonExpandedImpl.scala
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

import com.raquo.laminar.api.L.{Button => Peer, _}
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Component.{defaultEnabled, keyEnabled}
import de.sciss.lucre.swing.impl.ComponentHolder

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

    initProperty(keyEnabled , defaultEnabled)(v => component.ref.disabled = !v)
    super.initComponent()
  }
}
