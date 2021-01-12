/*
 *  TextFieldExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.TextField.{defaultColumns, defaultEditable, defaultText, keyColumns, keyEditable, keyText}
import de.sciss.lucre.swing.impl.ComponentHolder

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
    val columns   = ctx.getProperty[Ex[Int    ]](peer, keyColumns  ).fold(defaultColumns )(_.expand[T].value)
    val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

    //      println(s"text0 '$text0', columns $columns")

    deferTx {
      val c = new scala.swing.TextField(text0, columns)
      if (editable != defaultEditable) c.editable = editable
      component = c
    }
    super.initComponent()
  }
}
