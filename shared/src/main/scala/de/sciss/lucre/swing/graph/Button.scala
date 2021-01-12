/*
 *  Button.scala
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

import de.sciss.lucre.expr.graph.{Ex, Trig}
import de.sciss.lucre.expr.{Context, IControl, ITrigger}
import de.sciss.lucre.swing.graph.impl.ComponentImpl
import de.sciss.lucre.swing.graph.impl.ButtonExpandedImpl
import de.sciss.lucre.swing.graph.impl.ButtonClickedExpandedImpl
import de.sciss.lucre.Txn

object Button {

  def apply(text: Ex[String] = ""): Button = Impl(text)

  final case class Clicked(w: Button) extends Trig {
    type Repr[T <: Txn[T]] = ITrigger[T]

    override def productPrefix = s"Button$$Clicked"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws = w.expand[T]
      // XXX TODO: we should only keep one instance of `ButtonClickedExpandedImpl`
      // i.e. add `clicked` to `Repr`
      new ButtonClickedExpandedImpl[T](ws).init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends Button with ComponentImpl {
    override def productPrefix = "Button"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new ButtonExpandedImpl[T](this).initComponent()

    def clicked: Clicked = Clicked(this)

    def text: Ex[String] = text0
  }
}
trait Button extends Component {
  type C = View.Button

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  def clicked: Button.Clicked

  def text: Ex[String]
}