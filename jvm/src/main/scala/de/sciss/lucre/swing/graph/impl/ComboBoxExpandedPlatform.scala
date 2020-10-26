/*
 *  ComboBoxExpandedPlatform.scala
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
import de.sciss.lucre.swing.graph.ComboBox.{keyIndex, keyValueOption}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{IExpr, Txn}

// Note: we use SwingPlus because it allows us to use model instead of static seq
// which may come handy at some point
final class ComboBoxExpandedPlatform[T <: Txn[T], A](peer: ComboBox[A], tx0: T)(implicit ctx: Context[T])
  extends ComboBoxExpandedImpl[T, A](peer, tx0)
    with ComponentHolder[View.ComboBox[A]]
    with ComponentExpandedImpl[T]
    with ComboBox.Repr[T, A]  {

  def comboBox: View.ComboBox[A] = component

  protected def mkValueExpanded(init: (Int, Option[A])): IExpr[T, (Int, Option[A])] with TxnInit[T] = {
    import ctx.{cursor, targets}
    new ComboBoxValueExpandedImpl[T, A](comboBox, init)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val index   = ctx.getProperty[Ex[Int]]      (peer, keyIndex      ).fold(-1) (_.expand[T].value)
    val itemOpt = ctx.getProperty[Ex[Option[A]]](peer, keyValueOption).flatMap  (_.expand[T].value)
    val items0  = peer.items.expand[T].value
    deferTx {
      val c = new de.sciss.swingplus.ComboBox[A](items0)
      if (index >= 0 && index < items0.size) c.selection.index = index
      itemOpt.foreach { item => c.selection.item = item }
      component = c
    }

    initProperty(keyIndex       , 0               )(v => component.selection.index = v)
    initProperty(keyValueOption , Option.empty[A] )(v => v.foreach(component.selection.item = _))

    super.initComponent()
    _value.init()
    this
  }
}
