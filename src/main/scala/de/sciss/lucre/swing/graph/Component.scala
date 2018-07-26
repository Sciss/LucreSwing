/*
 *  Component.scala
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

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys

object Component {
  final case class Enabled(w: Component) extends Ex[Boolean] {
    override def productPrefix: String = s"Component$$Enabled" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt  = b.getProperty[Ex[Boolean]](w, keyEnabled)
        valueOpt.fold(Constant(defaultEnabled).expand[S])(_.expand[S])
    }

    def aux: List[Aux] = Nil
  }

  private[graph] final val keyEnabled     = "enabled"
  private[graph] final val defaultEnabled = true
}
trait Component extends Widget {
  // type C <: scala.swing.Component

  var enabled: Ex[Boolean]
}