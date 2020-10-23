package de.sciss.lucre.swing.graph.impl

import com.raquo.laminar.api.L.{Div => Peer, _}
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.FlowPanel
import de.sciss.lucre.swing.graph.FlowPanel.{defaultHGap, defaultVGap, keyHGap, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder

final class FlowPanelExpandedImpl[T <: Txn[T]](protected val peer: FlowPanel) extends View[T]
  with ComponentHolder[Peer] with PanelExpandedImpl[T] {

  type C = View.Component

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val hGap      = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[T].value)
    val vGap      = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[T].value)
//    val align     = ctx.getProperty[Ex[Int    ]](peer, keyAlign   ).fold(defaultAlign   )(_.expand[T].value)
    val contentsV: Seq[View[T]] = peer.contents.map(_.expand[T])
    deferTx {
//      val alignSwing = align match {
//        case graph.Align.Left     => Peer.Alignment.Left
//        case graph.Align.Right    => Peer.Alignment.Right
//        case graph.Align.Trailing => Peer.Alignment.Trailing
//        case graph.Align.Leading  => Peer.Alignment.Leading
//        case _                    => Peer.Alignment.Center
//      }
      val vec = contentsV.map { cv =>
        val cc = cv.component
        if (hGap == 0 && vGap == 0) cc else
          cc.amend(
            marginLeft    := s"${hGap}px",
            marginLeft    := s"${hGap}px",
            marginTop     := s"${vGap}px",
            marginBottom  := s"${vGap}px",
          )
      }
      val c = div(
        vec,
        cls := "lucre-flow-panel",
      )
      // fl: FlowLayout => fl.setAlignOnBaseline(true)
      component = c
    }
    super.initComponent()
  }
}
