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

package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Control, IControl}
import de.sciss.lucre.stm.Sys

trait Widget extends Control {
  type C <: scala.swing.Component

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]
}