/*
 *  Component.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.{IExpr, Txn}

object Component {
  object Enabled extends ProductReader[Enabled] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Enabled = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Component]()
      new Enabled(_w)
    }
  }
  final case class Enabled(w: Component) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"Component$$Enabled" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEnabled)
        valueOpt.fold(Const(defaultEnabled).expand[T])(_.expand[T])
    }
  }

  object Focusable extends ProductReader[Focusable] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Focusable = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Component]()
      new Focusable(_w)
    }
  }
  final case class Focusable(w: Component) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"Component$$Focusable" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
        val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyFocusable)
        valueOpt.fold(Const(defaultFocusable).expand[T])(_.expand[T])
    }
  }

  object Tooltip extends ProductReader[Tooltip] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Tooltip = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Component]()
      new Tooltip(_w)
    }
  }
  final case class Tooltip(w: Component) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"Component$$Tooltip" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
        val valueOpt = ctx.getProperty[Ex[String]](w, keyTooltip)
        valueOpt.fold(Const(defaultTooltip).expand[T])(_.expand[T])
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