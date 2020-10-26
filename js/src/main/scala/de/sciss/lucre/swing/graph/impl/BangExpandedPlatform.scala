/*
 *  BangExpandedPlatform.scala
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
import de.sciss.lucre.{Cursor, Txn}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import org.scalajs.dom

import scala.scalajs.js.timers

trait BangExpandedPlatform[T <: Txn[T]]
  extends ComponentExpandedImpl[T]
    with ComponentHolder[View.Component] {

  type C = View.Component

  // ---- abstract ----

  protected def bang(): Unit

  protected def cursor: Cursor[T]

  // ---- impl ----

  private[this] var timerHandle: timers.SetTimeoutHandle = null
  private[this] var active: L.Var[Boolean] = _
//  private[this] var active = false
//  private[this] var bangCircle: L.SvgElement = null

  protected def activate()(implicit tx: T): Unit = {
    deferTx {
      setActive(true)
      if (timerHandle != null) timers.clearTimeout(timerHandle)
      timerHandle = timers.setTimeout(200) {
        setActive(false)
      }
    }
  }

  private def setActive(value: Boolean): Unit =
    active.set(value)

//  private def setActive(value: Boolean): Unit = {
//    if (active != value) {
//      active = value
//      bangCircle.amend(
//        if (value) {
//          svg.className.apply ("lucre-bang-flash")
//        } else {
//          svg.className.remove("lucre-bang-flash")
//        }
//      )
//    }
//  }

  protected def guiDispose(): Unit =
    if (timerHandle != null) {
      timers.clearTimeout(timerHandle)
      timerHandle = null
    }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    deferTx {
      active = L.Var(false)
      val circ = svg.circle(
        svg.className.toggle("lucre-bang-flash") <-- active.signal,
        svg.className := "lucre-bang",
      )

      val obs = Observer[dom.MouseEvent] { _ =>
        cursor.step { implicit tx =>
          activate()
        }
      }

      val c = button(
        cls := "lucre-bang",
        onClick --> obs,
        svg.svg(
          svg.className := "lucre-bang",
          circ
        )
      )

      component   = c
//      bangCircle  = circ
    }
    super.initComponent()
  }
}