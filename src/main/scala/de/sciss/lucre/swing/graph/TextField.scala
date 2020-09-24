/*
 *  TextField.scala
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

import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
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
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"TextField$$Text" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[T]
      val valueOpt  = ctx.getProperty[Ex[String]](w, keyText)
      val value0    = valueOpt.fold[String](defaultText)(_.expand[T].value)
      new TextExpanded[T](ws, value0).init()
    }
  }

  private final class TextExpanded[T <: Txn[T]](ws: View.T[T, scala.swing.TextField], value0: String)
                                                (implicit protected val targets: ITargets[T], cursor: Cursor[T])
    extends IExpr[T, String]
      with IChangeGeneratorEvent[T, String] {

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

    def value(implicit tx: T): String = txValue.get(tx.peer)

    def changed: IChangeEvent[T, String] = this

    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): String =
      pull.resolveExpr(this)

    def init()(implicit tx: T): this.type = {
      deferTx {
        val c = ws.component
        val p = c.peer
        guiValue = c.text
        p.addActionListener (listenerA)
        p.addFocusListener  (listenerF)
      }
      this
    }

    def dispose()(implicit tx: T): Unit = {
      deferTx {
        val c = ws.component
        val p = c.peer
        p.removeActionListener(listenerA)
        p.removeFocusListener (listenerF)
      }
    }
  }

  final case class Columns(w: TextField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"TextField$$Columns" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Const(defaultColumns)).expand[T]
    }
  }

  final case class Editable(w: TextField) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"TextField$$Editable" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyEditable)
      valueOpt.getOrElse(Const(defaultEditable)).expand[T]
    }
  }

  private final class Expanded[T <: Txn[T]](protected val peer: TextField) extends View[T]
    with ComponentHolder[scala.swing.TextField] with ComponentExpandedImpl[T] {

    type C = scala.swing.TextField

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val textOpt   = ctx.getProperty[Ex[String]](peer, keyText).map(_.expand[T].value)
      val text0     = textOpt.orNull
      val columns   = ctx.getProperty[Ex[Int    ]](peer, keyColumns  ).fold(defaultColumns )(_.expand[T].value)
      val editable  = ctx.getProperty[Ex[Boolean]](peer, keyEditable ).fold(defaultEditable)(_.expand[T].value)

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
    
    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()

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

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  var columns: Ex[Int]

  def text: Model[String]

  var editable: Ex[Boolean]
}
