/*
 *  NumberField.scala
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

import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{IControl, Model}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.View

trait NumberField[A] extends Component {
  type C = de.sciss.audiowidgets.ParamField[A]

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]
  
  var min       : Ex[A]
  var max       : Ex[A]
  var step      : Ex[A]
  var editable  : Ex[Boolean]

  var unit      : Ex[String]
  var prototype : Ex[Seq[A]]

  def value     : Model[A]
}
