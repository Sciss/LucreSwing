/*
 *  CheckBox.scala
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

import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref

object CheckBox {

  def apply(text: Ex[String] = ""): CheckBox = Impl(text)

  private final class Expanded[T <: Txn[T]](protected val peer: CheckBox) extends View[T]
    with ComponentHolder[scala.swing.CheckBox] with ComponentExpandedImpl[T] {

    type C = scala.swing.CheckBox

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val text      = peer.text.expand[T]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      val selected  = ctx.getProperty[Ex[Boolean]](peer, keySelected).exists(_.expand[T].value)
      deferTx {
        val c = new scala.swing.CheckBox(text1)
        if (selected) c.selected = true
        component = c
      }

      initProperty(keySelected, defaultSelected)(component.selected = _)

      super.initComponent()
    }
  }

  private final class SelectedExpanded[T <: Txn[T]](ws: View.T[T, scala.swing.CheckBox], value0: Boolean)
                                                   (implicit protected val targets: ITargets[T], cursor: Cursor[T])
    extends IExpr[T, Boolean]
      with IChangeGeneratorEvent[T, Boolean] {

    private[this] val listener = new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = {
        val c       = ws.component
        val before  = guiValue
        val now     = c.selected
        val ch      = Change(before, now)
        if (ch.isSignificant) {
          guiValue    = now
          cursor.step { implicit tx =>
            txValue.set(now)(tx.peer)
            fire(ch)
          }
        }
      }
    }

    private[this] var guiValue: Boolean = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: T): Boolean = txValue.get(tx.peer)

    def changed: IChangeEvent[T, Boolean] = this

    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): Boolean =
      pull.resolveExpr(this)

    def init()(implicit tx: T): this.type = {
      deferTx {
        val c = ws.component
        guiValue = c.selected
        c.peer.addActionListener(listener)
      }
      this
    }

    def dispose()(implicit tx: T): Unit = {
      deferTx {
        val c = ws.component
        c.peer.removeActionListener(listener)
      }
    }
  }

  private final val defaultSelected = false
  private final val keySelected     = "selected"

  final case class Selected(w: CheckBox) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"CheckBox$$Selected" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws          = w.expand[T]
      val selectedOpt = ctx.getProperty[Ex[Boolean]](w, keySelected)
      val selected0   = selectedOpt.fold[Boolean](defaultSelected)(_.expand[T].value)
      import ctx.{cursor, targets}
      new SelectedExpanded[T](ws, selected0).init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends CheckBox with ComponentImpl { w =>
    override def productPrefix = "CheckBox"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()

    object selected extends Model[Boolean] {
      def apply(): Ex[Boolean] = Selected(w)

      def update(value: Ex[Boolean]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keySelected, value)
      }
    }

    def text: Ex[String] = text0
  }
}
trait CheckBox extends Component {
  type C = scala.swing.CheckBox

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  def text: Ex[String]

  def selected: Model[Boolean]
}