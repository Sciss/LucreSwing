/*
 *  Button.scala
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

import de.sciss.lucre.expr.Ex
import de.sciss.lucre.expr.ExOps._
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object Button {

  def apply(text: Ex[String] = ""): Button = Impl(text)

  private final class Expanded[S <: Sys[S]](protected val w: Button) extends View[S]
    with ComponentHolder[scala.swing.Button] with ComponentExpandedImpl[S] {

    type C = scala.swing.Button

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val text      = w.text.expand[S]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      deferTx {
        val c = new scala.swing.Button(text1)
        component = c
      }
      super.init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends Button with ComponentImpl {
    override def productPrefix = "Button"   // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

    def text: Ex[String] = text0
  }
}
trait Button extends Component {
  type C = scala.swing.Button

  def text: Ex[String]
}