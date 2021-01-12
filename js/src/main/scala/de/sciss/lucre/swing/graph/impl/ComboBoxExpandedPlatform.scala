/*
 *  ComboBoxExpandedPlatform.scala
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

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L._
import de.sciss.lucre.expr.Context
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
    implicit val tx: T = tx0
    val items0  = peer.items.expand[T].value
    new ComboBoxValueExpandedImpl[T, A](comboBox, items0, init)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
//    val index   = ctx.getProperty[Ex[Int]]      (peer, keyIndex      ).fold(-1) (_.expand[T].value)
//    val itemOpt = ctx.getProperty[Ex[Option[A]]](peer, keyValueOption).flatMap  (_.expand[T].value)
    val items0  = peer.items.expand[T].value
    deferTx {
      val options = items0.map { it =>
        val itS = it.toString
        L.option(
          itS,
          L.value := itS
        )
      }

      val c = select(
        cls := "lucre-combo-box",
        options,
      )

      component = c
    }

    initProperty(keyIndex, 0) { v =>
      component.ref.selectedIndex = v
    }
    initProperty(keyValueOption, Option.empty[A]) { v =>
      v.foreach { item =>
        val idx = items0.indexOf(item)
        component.ref.selectedIndex = idx
      }
    }

    super.initComponent()
    _value.init()
    this
  }
}
