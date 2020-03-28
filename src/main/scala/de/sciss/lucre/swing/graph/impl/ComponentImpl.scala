/*
 *  ComponentImpl.scala
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

import de.sciss.lucre.expr.Graph
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.graph.Component

trait ComponentImpl {
  _: Component =>

  def enabled: Ex[Boolean] = Component.Enabled(this)

  def enabled_=(value: Ex[Boolean]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Component.keyEnabled, value)
  }

  def focusable: Ex[Boolean] = Component.Focusable(this)

  def focusable_=(value: Ex[Boolean]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Component.keyFocusable, value)
  }

  def tooltip: Ex[String] = Component.Tooltip(this)

  def tooltip_=(value: Ex[String]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Component.keyTooltip, value)
  }
}
