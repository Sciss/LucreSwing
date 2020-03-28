/*
 *  Widget.scala
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

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.IControl
import de.sciss.lucre.expr.graph.Control
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.View

trait Widget extends Control {
  type C <: scala.swing.Component

  type Repr[S <: Sys[S]] <: View.T[S, C] with IControl[S]
}