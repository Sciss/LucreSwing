package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.{Disposable, IExpr, Txn}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.CheckBox.{defaultSelected, keySelected}
import de.sciss.lucre.swing.impl.ComponentHolder

final class CheckBoxExpandedImpl[T <: Txn[T]](protected val peer: CheckBox, tx0: T)(implicit protected val ctx: Context[T])
  extends CheckBox.Repr[T]
    with ComponentHolder[View.Component] with ComponentExpandedImpl[T] {

  var checkBox: View.CheckBox = _

  def selected: IExpr[T, Boolean] = _selected

  private[this] val _selected = {
    implicit val tx: T = tx0
    val selectedOpt = ctx.getProperty[Ex[Boolean]](peer, keySelected)
    val selected0   = selectedOpt.fold[Boolean](defaultSelected)(_.expand[T].value)
    import ctx.{cursor, targets}
    new CheckBoxSelectedExpandedImpl[T](this, selected0)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val text      = peer.text.expand[T]
    val text0     = text.value
    val text1     = if (text0.isEmpty) null else text0

    deferTx {
      val c = new scala.swing.CheckBox(text1)
      component = c
      checkBox  = c
    }

    initProperty(keySelected, defaultSelected)(checkBox.selected = _)

    super.initComponent()

    _selected.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    _selected.dispose()
  }
}
