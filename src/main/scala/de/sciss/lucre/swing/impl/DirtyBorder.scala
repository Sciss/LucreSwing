/*
 *  DirtyBorder.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre
package swing
package impl

import java.awt.geom.{AffineTransform, GeneralPath, Path2D}
import java.awt.{Color, Graphics, Graphics2D, RenderingHints, Shape}
import javax.swing.border.Border

import scala.swing.{Component, Insets, Swing}

object DirtyBorder {
  def apply(component: Component): DirtyBorder = {
    val bd0     = component.border
    val bd      = if (bd0 != null) bd0 else Swing.EmptyBorder
    val insets  = bd.getBorderInsets(component.peer)
    val p = new GeneralPath(Path2D.WIND_EVEN_ODD)
    mkShape(p)
    p.closePath()

    val extent = component.preferredSize.height - (insets.top + insets.bottom + 2)

    val shape = if (extent == 32) p else {
      val scale = extent/32f
      AffineTransform.getScaleInstance(scale, scale).createTransformedShape(p)
    }
    new Impl(shape, extent, component)
  }

  private def mkShape(p: Path2D): Unit = {
    // credits: Raphael Icons (http://raphaeljs.com/icons/), released under MIT license
    p.moveTo(25.31f, 2.872f)
    p.lineTo(21.925999f, 0.7449999f)
    p.curveTo(21.071999f, 0.20899987f, 19.946999f, 0.4669999f, 19.408998f, 1.3209999f)
    p.lineTo(18.074999f, 3.4439998f)
    p.lineTo(24.549f, 7.5099998f)
    p.lineTo(25.883999f, 5.3879995f)
    p.curveTo(26.42f, 4.533f, 26.164f, 3.407f, 25.31f, 2.872f)
    p.moveTo(6.555f, 21.786f)
    p.lineTo(13.028999f, 25.852f)
    p.lineTo(23.581f, 9.054f)
    p.lineTo(17.104f, 4.987f)
    p.lineTo(6.555f, 21.786f)
    p.moveTo(5.566f, 26.952f)
    p.lineTo(5.423f, 30.771f)
    p.lineTo(8.802f, 28.984f)
    p.lineTo(11.942f, 27.326f)
    p.lineTo(5.6960006f, 23.401001f)
    p.lineTo(5.566f, 26.952f)
  }

  private final class Impl(shape: Shape, extent: Int, component: Component) extends DirtyBorder {
    component.border = Swing.CompoundBorder(outside = component.border, inside = this)

    val isBorderOpaque = false
    
    private val fill = Color.gray

    private var _vis = false
    def visible = _vis
    def visible_=(value: Boolean): Unit = if (_vis != value) {
      _vis = value
      component.repaint()
    }

    def getBorderInsets(c: java.awt.Component): Insets = new Insets(0, extent + 2, 0, 0) // top left bottom right

    def paintBorder(c: java.awt.Component, g: Graphics, x0: Int, y0: Int, width: Int, height: Int): Unit = if (_vis) {
      val x     = x0 + 1
      val y     = y0 + 1 + ((height - extent /* icn.getIconHeight */) >> 1)
      val g2    = g.asInstanceOf[Graphics2D]
      val hints = g2.getRenderingHints
      val at    = g2.getTransform
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
      g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )
      g2.translate(x, y)
      g2.setPaint(fill)
      g2.fill(shape)
      g2.setTransform(at)
      g2.setRenderingHints(hints)      
    }
  }
}
trait DirtyBorder extends Border {
  var visible: Boolean
}