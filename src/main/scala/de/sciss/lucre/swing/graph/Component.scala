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

import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys

object Component {
  final case class Enabled(w: Component) extends Ex[Boolean] {
    override def productPrefix: String = s"Component$$Enabled" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEnabled)
        valueOpt.fold(Constant(defaultEnabled).expand[S])(_.expand[S])
    }
  }

  final case class Focusable(w: Component) extends Ex[Boolean] {
    override def productPrefix: String = s"Component$$Focusable" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyFocusable)
        valueOpt.fold(Constant(defaultFocusable).expand[S])(_.expand[S])
    }
  }

  final case class Tooltip(w: Component) extends Ex[String] {
    override def productPrefix: String = s"Component$$Tooltip" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, String] = {
        val valueOpt = ctx.getProperty[Ex[String]](w, keyTooltip)
        valueOpt.fold(Constant(defaultTooltip).expand[S])(_.expand[S])
    }
  }

  private[graph] final val keyEnabled       = "enabled"
  private[graph] final val keyFocusable     = "focusable"
  private[graph] final val keyTooltip       = "tooltip"
  private[graph] final val defaultEnabled   = true
  private[graph] final val defaultFocusable = true
  private[graph] final val defaultTooltip   = ""
}
trait Component extends Widget {
  // type C <: scala.swing.Component

  var enabled   : Ex[Boolean]
  var focusable : Ex[Boolean]
  var tooltip   : Ex[String ]
}