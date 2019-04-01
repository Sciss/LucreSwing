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

import de.sciss.lucre.expr.{Ex, Model}

import scala.collection.immutable.{Seq => ISeq}

trait NumberField[A] extends Component {
  type C = de.sciss.audiowidgets.ParamField[A]

  var min       : Ex[A]
  var max       : Ex[A]
  var step      : Ex[A]
  var editable  : Ex[Boolean]

  var unit      : Ex[String]
  var prototype : Ex[ISeq[A]]

  def value     : Model[A]
}
