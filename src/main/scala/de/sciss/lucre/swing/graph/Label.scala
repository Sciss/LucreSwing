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

import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.impl.ComponentHolder

object Label {
  def apply(text: Ex[String]): Label = Impl(text)

  def mk(text: Ex[String])(configure: Label => Unit): Label = {
    val w = apply(text)
    configure(w)
    w
  }

  private final class Expanded[S <: Sys[S]](text: IExpr[S, String]) extends View[S]
    with ComponentHolder[scala.swing.Label] {

    private[this] var obs: Disposable[S#Tx] = _

    def init()(implicit tx: S#Tx): this.type = {
      val text0 = text.value
      deferTx {
        component = new scala.swing.Label(text0)
      }
      obs = text.changed.react { implicit tx => ch =>
        deferTx {
          component.text = ch.now
        }
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit =
      obs.dispose()
  }

  private final case class Impl(text0: Ex[String]) extends Label {
    override def productPrefix: String = s"Label$$Impl"

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View[S] = {
      val textEx = text0.expand[S]
      new Expanded[S](textEx).init()
    }

    def text: Ex[String] = text0
  }
}
trait Label extends Widget {
  def text: Ex[String]
}