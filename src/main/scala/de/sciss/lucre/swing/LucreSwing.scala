/*
 *  LucreSwing.scala
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

package de.sciss.lucre.swing

import de.sciss.lucre.swing.graph.DropTarget

object LucreSwing {
  /** Registers all known types. */
  def init(): Unit = _init

  private lazy val _init: Unit = {
    DropTarget.init()
  }
}