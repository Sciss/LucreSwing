/*
 *  Label.scala
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
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object Label {
  def apply(text: Ex[String]): Label = Impl(text)

//  def mk(text: Ex[String])(configure: Label => Unit): Label = {
//    val w = apply(text)
//    configure(w)
//    w
//  }

  private final class Expanded[S <: Sys[S]](protected val w: Label) extends View[S]
    with ComponentHolder[scala.swing.Label] with ComponentExpandedImpl[S] {

    type C = scala.swing.Label

    private[this] var obs: Disposable[S#Tx] = _

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val text  = w.text.expand[S]
      val text0 = text.value
      deferTx {
        component = new scala.swing.Label(text0)
      }
      obs = text.changed.react { implicit tx => ch =>
        deferTx {
          component.text = ch.now
        }
      }
      super.init()
    }

    override def dispose()(implicit tx: S#Tx): Unit = {
      obs.dispose()
      super.dispose()
    }
  }

  private final case class Impl(text0: Ex[String]) extends Label with ComponentImpl {
    override def productPrefix: String = "Label" // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      new Expanded[S](this).init()
    }

    def text: Ex[String] = text0
  }
}
trait Label extends Component {
  type C = scala.swing.Label

  def text: Ex[String]
}