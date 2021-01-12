/*
 *  BorderPanelExpandedImpl.scala
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
import de.sciss.lucre.swing.graph.BorderPanel.{defaultHGap, defaultVGap, keyHGap, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder

final class BorderPanelExpandedImpl[T <: Txn[T]](protected val peer: BorderPanel)
  extends ComponentHolder[Peer] with PanelExpandedImpl[T] {

  type C = View.Component

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
    val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
    val north : View[T] = if (peer.north .isInstanceOf[Empty]) null else peer.north  .expand[T]
    val south : View[T] = if (peer.south .isInstanceOf[Empty]) null else peer.south  .expand[T]
    val west  : View[T] = if (peer.west  .isInstanceOf[Empty]) null else peer.west   .expand[T]
    val east  : View[T] = if (peer.east  .isInstanceOf[Empty]) null else peer.east   .expand[T]
    val center: View[T] = if (peer.center.isInstanceOf[Empty]) null else peer.center .expand[T]
    deferTx {
      val p = div(
        cls := "lucre-border-panel",
        styleAttr :=
          s"""grid-column-gap: ${hGap}px;
             |grid-row-gap: ${vGap}px;
             |""".stripMargin,
      )

      def mkField(orient: String, child: View[T]): Unit =
        if (child != null) {
          val cc = div(
            child.component,
            cls := s"lucre-border-panel-child lucre-border-panel-$orient",
          )
//          if (hGap != 0 || vGap != 0) {
//            cc.amend(
//              marginLeft    := s"${hGap}px",
//              marginRight   := s"${hGap}px",
//              marginTop     := s"${vGap}px",
//              marginBottom  := s"${vGap}px",
//            )
//          }
          p.amend(cc)
        }

      mkField("north" , north )
      mkField("south" , south )
      mkField("west"  , west  )
      mkField("east"  , east  )
      mkField("center", center)
      component = p
    }
    super.initComponent()
  }
}
