/*
 *  Label.scala
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

import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.Label.{defaultHAlign, defaultVAlign, keyHAlign, keyVAlign}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Disposable, Txn}

import scala.swing.{Swing, Label => Peer}

final class LabelExpandedImpl[T <: Txn[T]](protected val peer: Label) extends View[T]
  with ComponentHolder[Peer] with ComponentExpandedImpl[T] {

  type C = View.Component

  private[this] var obs: Disposable[T] = _

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val text    = peer.text.expand[T]
    val text0   = text.value
    val hAlign  = ctx.getProperty[Ex[Int]](peer, keyHAlign).fold(defaultHAlign)(_.expand[T].value)
    val vAlign  = ctx.getProperty[Ex[Int]](peer, keyVAlign).fold(defaultVAlign)(_.expand[T].value)

    deferTx {
      val hAlignSwing = hAlign match {
        case Align.Left     => scala.swing.Alignment.Left
        case Align.Center   => scala.swing.Alignment.Center
        case Align.Right    => scala.swing.Alignment.Right
        case Align.Trailing => scala.swing.Alignment.Trailing
        case _              => scala.swing.Alignment.Leading
      }
      // N.B. Scala Swing uses divergent default horizontal alignment of Center instead of Java Swing (CENTER)
      val c = new Peer(text0, Swing.EmptyIcon, hAlignSwing)
      if (vAlign != defaultVAlign) {
        c.verticalAlignment = vAlign match {
          case Align.Top      => scala.swing.Alignment.Top
          case Align.Bottom   => scala.swing.Alignment.Bottom
          case _              => scala.swing.Alignment.Center
        }
      }
      component = c
    }
    obs = text.changed.react { implicit tx => ch =>
      deferTx {
        component.text = ch.now
      }
    }
    super.initComponent()
  }

  override def dispose()(implicit tx: T): Unit = {
    obs.dispose()
    super.dispose()
  }
}

