/*
 *  TextField.scala
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

package de.sciss.lucre.swing.graph

import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}

import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref

object TextField {

  def apply(): TextField = Impl()

  def apply(columns: Ex[Int]): TextField = {
    val res = apply()
    res.columns = columns
    res
  }

  private final val keyText         = "text"
  private final val keyColumns      = "columns"
  private final val keyEditable     = "editable"
  private final val defaultText     = ""
  private final val defaultColumns  = 0
  private final val defaultEditable = true

  final case class Text(w: TextField) extends Ex[String] {
    type Repr[S <: Sys[S]] = IExpr[S, String]

    override def productPrefix: String = s"TextField$$Text" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val valueOpt  = ctx.getProperty[Ex[String]](w, keyText)
      val value0    = valueOpt.fold[String](defaultText)(_.expand[S].value)
      new TextExpanded[S](ws, value0).init()
    }
  }

  private final class TextExpanded[S <: Sys[S]](ws: View.T[S, scala.swing.TextField], value0: String)
                                                (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, String]
      with IGenerator[S, Change[String]] {

    private def commit(): Unit = {
      val c       = ws.component
      val before  = guiValue
      val now     = c.text
      val ch      = Change(before, now)
      if (ch.isSignificant) {
        guiValue    = now
        cursor.step { implicit tx =>
          txValue.set(now)(tx.peer)
          fire(ch)
        }
      }
    }

    private[this] val listenerA = new ActionListener {
      def actionPerformed(e: ActionEvent): Unit = commit()
    }

    private[this] val listenerF = new FocusListener {
      def focusLost(e: FocusEvent): Unit = commit()

      def focusGained(e: FocusEvent): Unit = ()
    }

    private[this] var guiValue: String = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): String = txValue.get(tx.peer)

    def changed: IEvent[S, Change[String]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[String]] =
      Some(pull.resolve)

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        val p = c.peer
        guiValue = c.text
        p.addActionListener (listenerA)
        p.addFocusListener  (listenerF)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val c = ws.component
        val p = c.peer
        p.removeActionListener(listenerA)
        p.removeFocusListener (listenerF)
      }
    }
  }

  final case class Columns(w: TextField) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"TextField$$Columns" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Const(defaultColumns)).expand[S]
    }
  }

  final case class Editable(w: TextField) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"TextField$$Editable" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[S]
    }
  }

  private final class Expanded[S <: Sys[S]](protected val peer: TextField) extends View[S]
    with ComponentHolder[scala.swing.TextField] with ComponentExpandedImpl[S] {

    type C = scala.swing.TextField

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val textOpt   = ctx.getProperty[Ex[String]](peer, keyText).map(_.expand[S].value)
      val text0     = textOpt.orNull
      val columns   = ctx.getProperty[Ex[Int    ]](peer, keyColumns  ).fold(defaultColumns )(_.expand[S].value)
      val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[S].value)

//      println(s"text0 '$text0', columns $columns")

      deferTx {
        val c = new scala.swing.TextField(text0, columns)
        if (editable != defaultEditable) c.editable = editable
        component = c
      }
      super.initComponent()
    }
  }

  private final case class Impl() extends TextField with ComponentImpl { w =>
    override def productPrefix: String = "TextField" // serialization
    
    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

    object text extends Model[String] {
      def apply(): Ex[String] = Text(w)

      def update(value: Ex[String]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyText, value)
      }
    }

    def columns: Ex[Int] = Columns(this)

    def columns_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyColumns, value)
    }

    def editable: Ex[Boolean] = Editable(this)

    def editable_=(value: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyEditable, value)
    }
  }
}
trait TextField extends Component {
  type C = scala.swing.TextField

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  var columns: Ex[Int]

  def text: Model[String]

  var editable: Ex[Boolean]
}
