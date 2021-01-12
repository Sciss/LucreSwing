/*
 *  GridPanelExpandedImpl.scala
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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.GridPanel.{defaultColumns, defaultHGap, defaultRows, defaultVGap, keyColumns, keyCompact, keyCompactColumns, keyCompactRows, keyHGap, keyRows, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.swingplus.{GridPanel => Peer}

final class GridPanelExpandedImpl[T <: Txn[T]](protected val peer: GridPanel) extends View[T]
  with ComponentHolder[Peer] with PanelExpandedImpl[T] {

  type C = View.Component // Peer

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val rows0           = ctx.getProperty[Ex[Int    ]](peer, keyRows    ).fold(defaultRows    )(_.expand[T].value)
    val columns         = ctx.getProperty[Ex[Int    ]](peer, keyColumns ).fold(defaultColumns )(_.expand[T].value)
    val compact         = ctx.getProperty[Ex[Boolean]](peer, keyCompact ).exists(_.expand[T].value)
    val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
    val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
    val compactRows     = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactRows   ).exists(_.expand[T].value)
    val compactColumns  = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactColumns).exists(_.expand[T].value)
    val rows            = if (rows0 == 0 && columns == 0) 1 else 0  // not allowed to have both zero
    val contents        = peer.contents.map(_.expand[T])
    deferTx {
      val vec           = contents.map(_.component)
      val p             = new Peer(rows0 = rows, cols0 = columns)
      p.compactRows     = compactRows
      p.compactColumns  = compactColumns
      p.hGap            = hGap
      p.vGap            = vGap
      p.contents      ++= vec
      component         = p
    }
    super.initComponent()
  }
}
