/*
 *  ComponentImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.expr.Ex

trait ComponentImpl {
  _: Component =>

  def enabled: Ex[Boolean] = Component.Enabled(this)

  def enabled_=(value: Ex[Boolean]): Unit = {
    val b = Graph.builder
    b.putProperty(this, Component.keyEnabled, value)
  }
}
