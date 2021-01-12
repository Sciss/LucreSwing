/*
 *  FlowPanelExpandedImpl.scala
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

package de.sciss.lucre.swing.graph.impl

import java.awt.FlowLayout

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.{View, graph}
import de.sciss.lucre.swing.graph.FlowPanel
import de.sciss.lucre.swing.graph.FlowPanel.{defaultAlign, defaultHGap, defaultVGap, keyAlign, keyHGap, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.{FlowPanel => Peer}

final class FlowPanelExpandedImpl[T <: Txn[T]](protected val peer: FlowPanel) extends View[T]
  with ComponentHolder[Peer] with PanelExpandedImpl[T] {

  type C = View.Component // Peer

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val hGap      = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
    val vGap      = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
    val align     = ctx.getProperty[Ex[Int    ]](peer, keyAlign   ).fold(defaultAlign   )(_.expand[T].value)
    val contentsV = peer.contents.map(_.expand[T])
    deferTx {
      val alignSwing = align match {
        case graph.Align.Left     => Peer.Alignment.Left
        case graph.Align.Right    => Peer.Alignment.Right
        case graph.Align.Trailing => Peer.Alignment.Trailing
        case graph.Align.Leading  => Peer.Alignment.Leading
        case _                    => Peer.Alignment.Center
      }
      val vec = contentsV.map(_.component)
      val c = new Peer(alignSwing)(vec: _*)
      c.hGap = hGap
      c.vGap = vGap
      c.peer.getLayout match {
        case fl: FlowLayout => fl.setAlignOnBaseline(true)
        case _ =>
      }
      component = c
    }
    super.initComponent()
  }
}
