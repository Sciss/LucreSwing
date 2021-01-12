/*
 *  ProgressBarExpandedImpl.scala
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
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.ProgressBar.{defaultLabel, defaultLabelPainted, defaultMax, defaultMin, keyLabel, keyLabelPainted, keyMax, keyMin, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder

final class ProgressBarExpandedImpl[T <: Txn[T]](protected val peer: ProgressBar) extends View[T]
  with ComponentHolder[scala.swing.ProgressBar] with ComponentExpandedImpl[T] {

  type C = View.Component // scala.swing.ProgressBar

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    deferTx {
      val c = new scala.swing.ProgressBar
      component = c
    }
    initProperty(keyMin         , defaultMin          )(component.min           = _)
    initProperty(keyMax         , defaultMax          )(component.max           = _)
    initProperty(keyValue       , defaultMin          )(component.value         = _)
    initProperty(keyLabel       , defaultLabel        )(component.label         = _)
    initProperty(keyLabelPainted, defaultLabelPainted )(component.labelPainted  = _)

    super.initComponent()
  }
}
