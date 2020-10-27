/*
 *  ProgressBarExpandedImpl.scala
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

import com.raquo.laminar.api.L._
import com.raquo.laminar.nodes.ReactiveHtmlElement
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.ProgressBar.{defaultMax, defaultMin, keyMax, keyMin, keyValue}
import de.sciss.lucre.swing.impl.ComponentHolder
import org.scalajs.dom

final class ProgressBarExpandedImpl[T <: Txn[T]](protected val peer: ProgressBar) extends View[T]
  with ComponentHolder[ReactiveHtmlElement[dom.html.Progress]] with ComponentExpandedImpl[T] {

  type C = View.Component

  private[this] var minVal  = ProgressBar.defaultMin
  private[this] var maxVal  = ProgressBar.defaultMax
  private[this] var curVal  = ProgressBar.defaultMin

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    minVal = ctx.getProperty[Ex[Int]](peer, keyMax   ).fold(defaultMin)(_.expand[T].value)
    maxVal = ctx.getProperty[Ex[Int]](peer, keyMax   ).fold(defaultMax)(_.expand[T].value)
    curVal = ctx.getProperty[Ex[Int]](peer, keyValue ).fold(minVal    )(_.expand[T].value)

    deferTx {
      val c = progress(
//        maxAttr := (maxVal - minVal).toString,
//        L.value := (curVal - minVal).toString,
      )
      c.ref.max   = (maxVal - minVal)
      c.ref.value = (curVal - minVal)
      component = c
    }
    initProperty(keyMin, defaultMin) { v =>
      minVal = v
      component.ref.max   = (maxVal - minVal)
      component.ref.value = (curVal - minVal)
//      component.amend(
//        maxAttr := (maxVal - minVal).toString,
//        L.value := (curVal - minVal).toString,
//      )
    }
    initProperty(keyMax, defaultMax) { v =>
      maxVal = v
      component.ref.max   = (maxVal - minVal)
//      component.amend(
//        maxAttr := (maxVal - minVal).toString,
//      )
    }
    initProperty(keyValue, defaultMin) { v =>
      curVal = v
      println("curVal = " + v)
      component.ref.value = (curVal - minVal).toDouble
//      component.amend(
//        L.value := (curVal - minVal).toString,
//      )
    }
//    initProperty(keyLabel       , defaultLabel        )(component.label         = _)
//    initProperty(keyLabelPainted, defaultLabelPainted )(component.labelPainted  = _)

    super.initComponent()
  }
}
