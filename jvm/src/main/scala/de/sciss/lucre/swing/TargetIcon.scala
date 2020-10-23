/*
 *  TargetIcon.scala
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

import java.awt.geom.{GeneralPath, Line2D, Path2D}
import java.awt.{BasicStroke, Color, RenderingHints}

import scala.swing.Graphics2D

/** A "target" icon useful for drag and drop targets. */
object TargetIcon {

  private[this] val strk1   = new BasicStroke(3f)
  private[this] val strk2   = new BasicStroke(6f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER)
  private[this] val strk3   = new BasicStroke(27.15094757f)
  private[this] val strk4   = new BasicStroke(68.07427216f)

  private[this] val gp      = new GeneralPath()
  private[this] val ln      = new Line2D.Float()

  private[this] val colr1E  = Color.black
  private[this] val colr1D  = new Color(0x60000000, true)
  private[this] val colr2E  = new Color(0x1A1A1A)
  private[this] val colr2D  = new Color(0x601A1A1A, true)
  private[this] val colr3E  = new Color(0xD40000)
  private[this] val colr3D  = new Color(0x60808080, true)
  private[this] val colr4E  = Color.white
  private[this] val colr4D  = new Color(0x60FFFFFF, true)
  private[this] val colr5   = /*if (enabled) */ new Color(0x4AFFFFFF, true) // else new Color(0x12FFFFFF, true)

  def paint(g: Graphics2D, x: Int, y: Int, extent: Double, enabled: Boolean): Unit = {
    val scale = extent.toDouble / 48   // natural size: 48x48

    val colr1   = if (enabled) colr1E else colr1D
    val colr2   = if (enabled) colr2E else colr2D
    val colr3   = if (enabled) colr3E else colr3D
    val colr4   = if (enabled) colr4E else colr4D

    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING  , RenderingHints.VALUE_ANTIALIAS_ON)
    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE )

    val atOrig    = g.getTransform
    val strkOrig  = g.getStroke

    g.translate(x, y)
    g.scale(scale, scale)
    // g.setColor(Color.blue)
    // g.fillRect(0, 0, 48, 48)

    g.translate(-21.84933,-14.3582)
    g.scale(0.06367963, 0.06367963)

    val at0     = g.getTransform

    // path2830
    val p2816 = gp
    gp.reset()
    gp.setWindingRule(Path2D.WIND_NON_ZERO)
    p2816.moveTo(949.76306f, 447.8673f)
    p2816.curveTo(949.76306f, 644.17706f, 790.62256f, 803.3175f, 594.31287f, 803.3175f)
    p2816.curveTo(398.0031f, 803.3175f, 238.86264f, 644.177f, 238.86264f, 447.86728f)
    p2816.curveTo(238.86264f, 251.55754f, 398.0031f, 92.41705f, 594.31287f, 92.41705f)
    p2816.curveTo(790.62256f, 92.41705f, 949.76306f, 251.55754f, 949.76306f, 447.86728f)
    p2816.closePath()
    g.translate(118.70759,149.23512)
    g.scale(1.011744,1.011744)
    g.setColor(colr1)
    g.fill(p2816)
    g.setTransform(at0)

    // path2816
    // = path2830
    g.translate(125.68719,154.49487)
    g.setColor(colr4)
    g.fill(p2816)
    g.setStroke(strk1)
    g.setColor(colr1)
    g.draw(p2816)
    g.setTransform(at0)

    // path2818
    // style="fill:#1a1a1a;fill-opacity:1;fill-rule:evenodd;stroke:none"
    // transform="matrix(0.86837031,0,0,0.86837031,203.9164,213.4475)"
    // == SAME AS path2830
    g.translate(203.9164,213.4475)
    g.scale(0.86837031,0.86837031)
    g.setColor(colr2)
    g.fill(p2816)
    g.setTransform(at0)

    // path2820
    // == path2830
    g.translate(275.03386,267.04081)
    g.scale(0.74870696,0.74870696)
    g.setColor(colr4)
    g.fill(p2816)
    g.setTransform(at0)

    // path2822
    // == path2830
    g.translate(362.75873,333.14927)
    g.scale(0.60109972,0.60109972)
    g.setColor(colr2)
    g.fill(p2816)
    g.setTransform(at0)

    // path2824
    // == path2830
    g.translate(436.23344,388.51896)
    g.scale(0.47747003,0.47747003)
    g.setColor(colr4)
    g.fill(p2816)
    g.setTransform(at0)

    // path2826
    // == path2830
    g.translate(519.20381,451.04448)
    g.scale(0.33786279,0.33786279)
    g.setColor(colr3)
    g.fill(p2816)
    g.setTransform(at0)

    // path3612
    val p3612 = ln
    p3612.setLine(719.4313f, 95.260666f, 719.4313f, 803.755f)
    g.translate(0,152.36218)
    g.setColor(colr1)
    g.setStroke(strk2)
    g.draw(p3612)
    g.setTransform(at0)

    // path3614
    val p3614 = ln
    p3614.setLine(365.1841f, 601.87f, 1073.6785f, 601.87f)
    // g.setColor(colr1)
    g.setStroke(strk2)
    g.draw(p3614)

    // path3616
    // == path2830
    g.translate(588.66472,503.38942)
    g.scale(0.22098677,0.22098677)
    // g.setColor(colr1)
    g.setStroke(strk3)
    g.draw(p2816)
    g.setTransform(at0)

    // path3618
    // == path2830
    g.translate(667.61783,562.88758)
    g.scale(0.08813903,0.08813903)
    // g.setColor(colr1)
    g.setStroke(strk4)
    g.draw(p2816)
    g.setTransform(at0)

    // path3622
    val p3622 = gp
    gp.reset()
    gp.setWindingRule(Path2D.WIND_EVEN_ODD)
    p3622.moveTo(720.0f, 94.5625f)
    p3622.curveTo(529.3595f, 94.5625f, 373.7649f, 244.63683f, 364.9375f, 433.09375f)
    p3622.curveTo(600.5692f, 350.96393f, 837.05774f, 335.65155f, 1074.9688f, 431.3125f)
    p3622.curveTo(1065.2448f, 243.69492f, 910.0374f, 94.5625f, 720.0f, 94.5625f)
    p3622.closePath()
    g.translate(0,152.36218)
    g.setColor(colr5)
    g.setStroke(strk4)
    g.fill(p3622)
    // g.setTransform(at0)

    g.setTransform(atOrig)
    g.setStroke(strkOrig)
  }
}