/*
 *  Component.scala
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

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IExpr}
import de.sciss.lucre.stm.Sys

object Component {
  final case class Enabled(w: Component) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"Component$$Enabled" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEnabled)
        valueOpt.fold(Const(defaultEnabled).expand[S])(_.expand[S])
    }
  }

  final case class Focusable(w: Component) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"Component$$Focusable" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyFocusable)
        valueOpt.fold(Const(defaultFocusable).expand[S])(_.expand[S])
    }
  }

  final case class Tooltip(w: Component) extends Ex[String] {
    type Repr[S <: Sys[S]] = IExpr[S, String]

    override def productPrefix: String = s"Component$$Tooltip" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
        val valueOpt = ctx.getProperty[Ex[String]](w, keyTooltip)
        valueOpt.fold(Const(defaultTooltip).expand[S])(_.expand[S])
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