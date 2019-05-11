/*
 *  Widget.scala
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

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.IControl
import de.sciss.lucre.expr.graph.Control
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.View

import scala.language.higherKinds

trait Widget extends Control {
  type C <: scala.swing.Component

  type Repr[S <: Sys[S]] <: View.T[S, C] with IControl[S]
}