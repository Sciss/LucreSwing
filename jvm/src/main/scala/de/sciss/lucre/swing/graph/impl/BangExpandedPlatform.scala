/*
 *  BangExpandedPlatform.scala
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

import java.awt.Dimension
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.geom.Ellipse2D

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import javax.swing.{JButton, Timer}

import scala.swing.Graphics2D
import scala.swing.event.ButtonClicked

trait BangExpandedPlatform[T <: Txn[T]]
  extends ComponentExpandedImpl[T]
  with ComponentHolder[scala.swing.Button] {

  type C = View.Component

  // ---- abstract ----

  protected def bang(): Unit

  // ---- impl ----

  private[this] var active = false
  private[this] var timer: Timer = _

  protected def activate()(implicit tx: T): Unit = {
    deferTx {
      setActive(true)
      timer.restart()
    }
  }

  private def setActive(value: Boolean): Unit =
    if (active != value) {
      active = value
      val c = component
      c.repaint()
      // in Linux, repainting may be quite strongly delayed
      // if the mouse or keyboard is not active.
      // Thus force graphics state sync
      c.toolkit.sync()
    }

  protected def guiDispose(): Unit =
    timer.stop()

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    deferTx {
      timer = new Timer(200, new ActionListener {
        def actionPerformed(e: ActionEvent): Unit =
          setActive(false)
      })
      timer.setRepeats(false)

      val c: scala.swing.Button = new scala.swing.Button {
        private[this] val el = new Ellipse2D.Double()

        override lazy val peer: JButton = new JButton(" ") with SuperMixin {
          // XXX TODO --- hack to avoid too narrow buttons under certain look-and-feel
          override def getPreferredSize: Dimension = {
            val d = super.getPreferredSize
            if (!isPreferredSizeSet) {
              d.width = math.max(d.width, d.height)
            }
            d
          }
        }

        override protected def paintComponent(g: Graphics2D): Unit = {
          super.paintComponent(g)
          g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
          g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
            java.awt.RenderingHints.VALUE_STROKE_PURE)
          g.setColor(foreground)
          val p = peer
          val w = p.getWidth
          val h = p.getHeight
          val ext1 = math.max(0, math.min(w, h) - 14)
          val _el = el
          _el.setFrame((w - ext1) * 0.5, (h - ext1) * 0.5, ext1, ext1)
          g.draw(_el)
          //            if (active) {
          //              val ext2 = math.max(0, ext1 - 5)
          //              _el.setFrame((w - ext2) * 0.5, (h - ext2) * 0.5, ext2, ext2)
          //              g.fill(_el)
          //            }
          if (active) g.fill(_el) else g.draw(_el)
        }

        reactions += {
          case ButtonClicked(_) => bang()
        }
      }
      component = c
    }
    super.initComponent()
  }
}
