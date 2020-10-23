/*
 *  Button.scala
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

package de.sciss.lucre.swing.graph

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.expr.graph.{Ex, Trig}
import de.sciss.lucre.expr.{Context, IControl, ITrigger}
import de.sciss.lucre.impl.IGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IEvent, IPull, ITargets, Txn}

object Button {

  def apply(text: Ex[String] = ""): Button = Impl(text)

  private final class Expanded[T <: Txn[T]](protected val peer: Button) extends View[T]
    with ComponentHolder[scala.swing.Button] with ComponentExpandedImpl[T] {

    type C = scala.swing.Button

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val text      = peer.text.expand[T]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      deferTx {
        val c = new scala.swing.Button(text1)
        component = c
      }
      super.initComponent()
    }
  }

  private final class ClickedExpanded[T <: Txn[T]](ws: View.T[T, scala.swing.Button])
                                                  (implicit protected val targets: ITargets[T], cursor: Cursor[T])
    extends ITrigger[T] with IGeneratorEvent[T, Unit] {

    def changed: IEvent[T, Unit] = this

    private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[Unit] = Trig.Some

    private[this] val listenerA = new ActionListener {
      def actionPerformed(e: ActionEvent): Unit =
        cursor.step { implicit tx =>
          fire(())
        }
    }

    def init()(implicit tx: T): this.type = {
      deferTx {
        val c = ws.component
        c.peer.addActionListener(listenerA)
      }
      this
    }

    def dispose()(implicit tx: T): Unit = {
      deferTx {
        val c = ws.component
        c.peer.removeActionListener(listenerA)
      }
    }
  }

  final case class Clicked(w: Button) extends Trig {
    type Repr[T <: Txn[T]] = ITrigger[T]

    override def productPrefix = s"Button$$Clicked"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws = w.expand[T]
      new ClickedExpanded[T](ws).init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends Button with ComponentImpl {
    override def productPrefix = "Button"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()

    def clicked: Clicked = Clicked(this)

    def text: Ex[String] = text0
  }
}
trait Button extends Component {
  type C = scala.swing.Button

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  def clicked: Button.Clicked

  def text: Ex[String]
}