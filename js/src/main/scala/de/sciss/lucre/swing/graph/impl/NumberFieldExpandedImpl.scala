/*
 *  NumberFieldExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import com.raquo.laminar.api.L
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.graph.Component.{defaultEnabled, keyEnabled}
import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}

abstract class NumberFieldExpandedImpl[T <: Txn[T], A, P <: Component](protected val peer: P, tx0: T)
                                                                      (implicit ctx: Context[T])
  extends View[T] with IControl[T]
    with ComponentExpandedImpl[T] {

  type C = View.Component

  def value: IExpr[T, A] = _value

  protected def input: L.Input

  protected def mkValueExpanded(value0: A)(implicit tx: T, targets: ITargets[T],
                                           cursor: Cursor[T]): IExpr[T, A] with TxnInit[T]

  protected def keyValue    : String
  protected def defaultValue: A

  private[this] val _value = {
    implicit val tx: T = tx0
    val valueOpt = ctx.getProperty[Ex[A]](peer, keyValue)
    val value0   = valueOpt.fold[A](defaultValue)(_.expand[T].value)
    import ctx.{cursor, targets}
    mkValueExpanded(value0)
  }

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    initProperty(keyEnabled , defaultEnabled)(v => input.ref.disabled = !v)

    super.initComponent()

    _value.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    value.dispose()
  }
}
