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

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val text      = w.text.expand[S]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
//      val selected  = b.getProperty[Ex[Boolean]](w, keySelected).exists(_.expand[S].value)
      deferTx {
        val c = new scala.swing.Button(text1)
//        if (selected) c.selected = true
        component = c
      }
      super.init()
    }

    //    def dispose()(implicit tx: S#Tx): Unit = super.dispose()
  }

  private final case class Impl(text0: Ex[String]) extends Button with ComponentImpl {
    override def productPrefix = "Button"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
      new Expanded[S](this).init()

//    def selected: Ex[Boolean] = Selected(this)
//
//    def selected_=(value: Ex[Boolean]): Unit = {
//      val b = Graph.builder
//      b.putProperty(this, keySelected, value)
//    }

    def text: Ex[String] = text0
  }
}
trait Button extends Component {
  type C = scala.swing.Button

  def text: Ex[String]

//  var selected: Ex[Boolean]
}