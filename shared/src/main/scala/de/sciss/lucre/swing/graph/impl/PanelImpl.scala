/*
 *  PanelImpl.scala
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

package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.Graph
import de.sciss.lucre.swing.graph.{Border, Panel}

trait PanelImpl extends ComponentImpl {
  self: Panel =>

  def border: Ex[Border] = Panel.Border(this)

  def border_=(value: Ex[Border]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Panel.keyBorder, value)
  }
}
