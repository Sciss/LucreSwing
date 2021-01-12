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

import com.raquo.laminar.api.L.{Div => Peer, _}
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.GridPanel.{defaultColumns, defaultHGap, defaultRows, defaultVGap, keyColumns, keyCompact, keyCompactColumns, keyCompactRows, keyHGap, keyRows, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder

final class GridPanelExpandedImpl[T <: Txn[T]](protected val peer: GridPanel) extends View[T]
  with ComponentHolder[Peer] with PanelExpandedImpl[T] {

  type C = View.Component // Peer

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val rows0           = ctx.getProperty[Ex[Int    ]](peer, keyRows    ).fold(defaultRows    )(_.expand[T].value)
    val cols0           = ctx.getProperty[Ex[Int    ]](peer, keyColumns ).fold(defaultColumns )(_.expand[T].value)
    val compact         = ctx.getProperty[Ex[Boolean]](peer, keyCompact ).exists(_.expand[T].value)
    val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
    val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
    val compactRows     = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactRows   ).exists(_.expand[T].value)
    val compactColumns  = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactColumns).exists(_.expand[T].value)
//    val rows            = if (rows0 == 0 && columns0 == 0) 1 else 0  // not allowed to have both zero
    val contents        = peer.contents.map(_.expand[T])
    deferTx {
      val vec     = contents.map(_.component)
      val numComp = vec.size

      val (rows, cols) = if (rows0 > 0) {
        val cols1 = (numComp + rows0 - 1) / rows0
        (rows0, cols1)
      } else if (cols0 > 0) {
        val rows1 = (numComp + cols0 - 1) / cols0
        (rows1, cols0)
      } else {
        (0, 0)
      }

      def mkPercent(n: Int): String = {
        val st = (10000 / n).toString
        val i  = st.length - 2
        s"${st.substring(0, i)}.${st.substring(i)}%"
      }

      // XXX TODO 'auto' is an approximation of the desired behaviour,
      // it does not take into account if a component 'wants' to take up
      // more space or not. This needs to be added as meta-data to the
      // components themselves, I guess.

      val sColTpe = if (cols == 0) "" else {
        val colTpe = if (compactColumns) "max-content" /*"auto"*/ else mkPercent(cols)
        val colTpeS = (s" $colTpe") * cols
        s"grid-template-columns:$colTpeS;\n"
      }
      val sRowTpe = if (compactRows || rows == 0) "" else {
        val rowTpe  = mkPercent(rows)
        val rowTpeS = (s" $rowTpe") * rows
        s"grid-template-rows:$rowTpeS;\n"
      }

      val p = div(
        vec,
        cls := "lucre-grid-panel",
        styleAttr := s"${sRowTpe}${sColTpe}grid-column-gap: ${hGap}px;\ngrid-row-gap: ${vGap}px;",
      )

      component = p
    }
    super.initComponent()
  }
}
