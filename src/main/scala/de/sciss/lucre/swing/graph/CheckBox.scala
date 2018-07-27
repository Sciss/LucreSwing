/*
 *  CheckBox.scala
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

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.ExOps._
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref

object CheckBox {

  def apply(text: Ex[String] = ""): CheckBox = Impl(text)

//  def mk(configure: CheckBox => Unit): CheckBox = {
//    val w = apply()
//    configure(w)
//    w
//  }

  private final class Expanded[S <: Sys[S]](protected val w: CheckBox) extends View[S]
    with ComponentHolder[scala.swing.CheckBox] with ComponentExpandedImpl[S] {

    type C = scala.swing.CheckBox

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val text      = w.text.expand[S]
      val text0     = text.value
      val text1     = if (text0.isEmpty) null else text0
      val selected  = b.getProperty[Ex[Boolean]](w, keySelected).exists(_.expand[S].value)
      deferTx {
        val c = new scala.swing.CheckBox(text1)
        if (selected) c.selected = true
        component = c
      }
      super.init()
    }

//    def dispose()(implicit tx: S#Tx): Unit = super.dispose()
  }

  private final class SelectedExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.CheckBox], value0: Boolean)
                                                   (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Boolean]
      with IGenerator[S, Change[Boolean]] {

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

    def changed: IEvent[S, Change[Boolean]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Boolean]] =
      Some(pull.resolve[Change[Boolean]])

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
    override def productPrefix: String = s"CheckBox$$Selected" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = ctx match {
      case b: Widget.Builder[S] =>
        import b.{cursor, targets}
        val ws          = w.expand[S](b, tx)
        val selectedOpt = b.getProperty[Ex[Boolean]](w, keySelected)
        val selected0   = selectedOpt.fold[Boolean](defaultSelected)(_.expand[S].value)
        new SelectedExpanded[S](ws, selected0).init()
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl(text0: Ex[String]) extends CheckBox with ComponentImpl {
    override def productPrefix = "CheckBox"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
      new Expanded[S](this).init()

    def selected: Ex[Boolean] = Selected(this)

    def selected_=(value: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keySelected, value)
    }

    def text: Ex[String] = text0
  }
}
trait CheckBox extends Component {
  type C = scala.swing.CheckBox

  def text: Ex[String]

  var selected: Ex[Boolean]
}