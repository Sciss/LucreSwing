/*
 *  TextFieldExpandedImpl.scala
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

import com.raquo.laminar.api.L._
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.TextField.{defaultEditable, defaultText, keyEditable, keyText}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{IExpr, Txn}

final class TextFieldExpandedImpl[T <: Txn[T]](protected val peer: TextField, tx0: T)(implicit ctx: Context[T])
  extends View[T]
    with ComponentHolder[View.TextField] with ComponentExpandedImpl[T] with TextField.Repr[T] {

  //  type C = View.TextField // scala.swing.TextField

  def textField: View.TextField = component

  def text: IExpr[T, String] = _text

  private[this] val _text = {
    implicit val tx: T = tx0
    val valueOpt = ctx.getProperty[Ex[String]](peer, keyText)
    val value0   = valueOpt.fold[String](defaultText)(_.expand[T].value)
    import ctx.{cursor, targets}
    new TextFieldTextExpandedImpl[T](textField, value0)
  }
  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    val textOpt   = ctx.getProperty[Ex[String]](peer, keyText).map(_.expand[T].value)
    val text0     = textOpt.orNull
//    val columns   = ctx.getProperty[Ex[Int    ]](peer, keyColumns  ).fold(defaultColumns )(_.expand[T].value)
    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

    //      println(s"text0 '$text0', columns $columns")

    deferTx {
      val c = input(
        text0,
        cls := "lucre-text-field",
        contentEditable := editable,
      )
      component = c
    }
    super.initComponent()
    _text.init()
    this
  }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    text.dispose()
  }
}
