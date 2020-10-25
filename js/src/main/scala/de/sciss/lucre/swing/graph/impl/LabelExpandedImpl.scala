/*
 *  LabelExpandedImpl.scala
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

import com.raquo.laminar.api.L.{Label => Peer, _}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Label.{defaultHAlign, keyHAlign}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Disposable, Txn}

final class LabelExpandedImpl[T <: Txn[T]](protected val peer: Label) extends View[T]
  with ComponentHolder[Peer] with ComponentExpandedImpl[T] {

  type C = View.Component

  private[this] var obs: Disposable[T] = _

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val text    = peer.text.expand[T]
    val text0   = text.value
    val hAlign  = ctx.getProperty[Ex[Int]](peer, keyHAlign).fold(defaultHAlign)(_.expand[T].value)
//    val vAlign  = ctx.getProperty[Ex[Int]](peer, keyVAlign).fold(defaultVAlign)(_.expand[T].value)

    deferTx {
      val hAlignPeer = hAlign match {
        case Align.Left     => "left"   // left
        case Align.Center   => "center" // center
        case Align.Right    => "right"  // right
        case Align.Trailing => "right"  // right
        case _              => "left"   // left
      }
      val c = label(
        text0,
        cls := "lucre-label",
        textAlign := hAlignPeer
      )

//      val c = new Peer(text0, Swing.EmptyIcon, hAlignSwing)
//      if (vAlign != defaultVAlign) {
//        c.verticalAlignment = vAlign match {
//          case Align.Top      => scala.swing.Alignment.Top
//          case Align.Bottom   => scala.swing.Alignment.Bottom
//          case _              => scala.swing.Alignment.Center
//        }
//      }
      component = c
    }

    obs = text.changed.react { implicit tx => ch =>
      deferTx {
        component.ref.textContent = ch.now
      }
    }

    initProperty(Component.keyEnabled, Component.defaultEnabled)(p => component.amend(disabled := !p))
    super.initComponent()
  }

  override def dispose()(implicit tx: T): Unit = {
    obs.dispose()
    super.dispose()
  }
}

