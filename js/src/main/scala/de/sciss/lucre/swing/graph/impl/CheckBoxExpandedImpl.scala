/*
 *  CheckBoxExpandedImpl.scala
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

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L._
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.CheckBox.{defaultSelected, keySelected}
import de.sciss.lucre.swing.graph.Component.{defaultEnabled, keyEnabled}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{IExpr, Txn}

final class CheckBoxExpandedImpl[T <: Txn[T]](protected val peer: CheckBox, tx0: T)(implicit ctx: Context[T])
  extends CheckBox.Repr[T]
    with ComponentHolder[L.Label] with ComponentExpandedImpl[T] {

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
      val c = input(
        cls     := "lucre-checkbox",
        `type`  := "checkbox",
      )

      val lb = label(
        text1,
        cls := "lucre-checkbox",
        c
      )

      component = lb
      checkBox  = c
    }

    initProperty(keySelected, defaultSelected)(v => checkBox.ref.checked  = v)
    initProperty(keyEnabled , defaultEnabled )(v => checkBox.ref.disabled = !v)

    super.initComponent()

    _selected.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    _selected.dispose()
  }
}
