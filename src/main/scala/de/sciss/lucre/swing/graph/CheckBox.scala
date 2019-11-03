/*
 *  CheckBox.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.event.impl.IChangeGenerator
import de.sciss.lucre.event.{IChangeEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, Graph, IControl, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref

object CheckBox {

  def apply(text: Ex[String] = ""): CheckBox = Impl(text)

  private final class Expanded[S <: Sys[S]](protected val peer: CheckBox) extends View[S]
    with ComponentHolder[scala.swing.CheckBox] with ComponentExpandedImpl[S] {

    type C = scala.swing.CheckBox

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val text      = peer.text.expand[S]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      val selected  = ctx.getProperty[Ex[Boolean]](peer, keySelected).exists(_.expand[S].value)
      deferTx {
        val c = new scala.swing.CheckBox(text1)
        if (selected) c.selected = true
        component = c
      }

      initProperty(keySelected, defaultSelected)(component.selected = _)

      super.initComponent()
    }
  }

  private final class SelectedExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.CheckBox], value0: Boolean)
                                                   (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Boolean]
      with IChangeGenerator[S, Boolean] {

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

    def value(implicit tx: S#Tx): Boolean = txValue.get(tx.peer)

    def changed: IChangeEvent[S, Boolean] = this

    private[lucre] def pullChange(pull: IPull[S])(implicit tx: S#Tx, phase: IPull.Phase): Boolean =
      pull.resolveExpr(this)

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        guiValue = c.selected
        c.peer.addActionListener(listener)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val c = ws.component
        c.peer.removeActionListener(listener)
      }
    }
  }

  private final val defaultSelected = false
  private final val keySelected     = "selected"

  final case class Selected(w: CheckBox) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"CheckBox$$Selected" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val ws          = w.expand[S]
      val selectedOpt = ctx.getProperty[Ex[Boolean]](w, keySelected)
      val selected0   = selectedOpt.fold[Boolean](defaultSelected)(_.expand[S].value)
      import ctx.{cursor, targets}
      new SelectedExpanded[S](ws, selected0).init()
    }
  }

  private final case class Impl(text0: Ex[String]) extends CheckBox with ComponentImpl { w =>
    override def productPrefix = "CheckBox"   // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

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

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  def text: Ex[String]

  def selected: Model[Boolean]
}