/*
 *  Button.scala
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

package de.sciss.lucre.swing
package graph

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.expr.Ex.Context
import de.sciss.lucre.expr.{Ex, IControl, ITrigger, Trig}
import de.sciss.lucre.expr.ExOps._
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object Button {

  def apply(text: Ex[String] = ""): Button = Impl(text)

  private final class Expanded[S <: Sys[S]](protected val peer: Button) extends View[S]
    with ComponentHolder[scala.swing.Button] with ComponentExpandedImpl[S] {

    type C = scala.swing.Button

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val text      = peer.text.expand[S]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      deferTx {
        val c = new scala.swing.Button(text1)
        component = c
      }
      super.initComponent()
    }
  }

  private final class ClickedExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.Button])
                                                  (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends ITrigger[S] with IGenerator[S, Unit] {

    def changed: IEvent[S, Unit] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Unit] = Trig.Some

    private[this] val listenerA = new ActionListener {
      def actionPerformed(e: ActionEvent): Unit =
        cursor.step { implicit tx =>
          fire(())
        }
    }

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        c.peer.addActionListener(listenerA)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val c = ws.component
        c.peer.removeActionListener(listenerA)
      }
    }
  }

  final case class Clicked(w: Button) extends Trig {
    override def productPrefix = s"Button$$Clicked"   // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): ITrigger[S] = {
      import ctx.{targets, cursor}
      val ws = w.expand[S]
      new ClickedExpanded[S](ws).init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends Button with ComponentImpl {
    override def productPrefix = "Button"   // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

    def clicked = Clicked(this)

    def text: Ex[String] = text0
  }
}
trait Button extends Component {
  type C = scala.swing.Button

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  def clicked: Button.Clicked

  def text: Ex[String]
}