/*
 *  NumberField.scala
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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{IControl, Model}
import de.sciss.lucre.swing.View

trait NumberField[A] extends Component {
  type C = de.sciss.audiowidgets.ParamField[A]

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]
  
  var min       : Ex[A]
  var max       : Ex[A]
  var step      : Ex[A]
  var editable  : Ex[Boolean]

  var unit      : Ex[String]
  var prototype : Ex[Seq[A]]

  def value     : Model[A]
}
