package de.sciss.lucre.swing
package graph
package impl

import java.awt.BorderLayout

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.BorderPanel.{defaultHGap, defaultVGap, keyHGap, keyVGap}
import de.sciss.lucre.swing.impl.ComponentHolder

final class BorderPanelExpandedImpl[T <: Txn[T]](protected val peer: BorderPanel)
  extends ComponentHolder[scala.swing.BorderPanel] with PanelExpandedImpl[T] {

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
      val p     = new scala.swing.BorderPanel
      val lay   = p.layoutManager
      lay.setHgap(hGap)
      lay.setVgap(vGap)
      val peer  = p.peer
      if (north   != null) peer.add(north .component.peer, BorderLayout.NORTH )
      if (south   != null) peer.add(south .component.peer, BorderLayout.SOUTH )
      if (west    != null) peer.add(west  .component.peer, BorderLayout.WEST  )
      if (east    != null) peer.add(east  .component.peer, BorderLayout.EAST  )
      if (center  != null) peer.add(center.component.peer, BorderLayout.CENTER)
      component = p
    }
    super.initComponent()
  }
}
