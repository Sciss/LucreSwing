/*
 *  PanelImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.Graph
import de.sciss.lucre.swing.graph.{Border, Panel}

trait PanelImpl extends ComponentImpl {
  _: Panel =>

  def border: Ex[Border] = Panel.Border(this)

  def border_=(value: Ex[Border]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Panel.keyBorder, value)
  }
}
