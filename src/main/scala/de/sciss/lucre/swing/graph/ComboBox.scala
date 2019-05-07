/*
 *  ComboBox.scala
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

import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, IControl, IExpr, Model}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComboBoxIndexExpandedImpl, ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.event.SelectionChanged

// Note: we use SwingPlus because it allows us to use model instead of static seq
// which may come handy at some point
object ComboBox {

  def apply[A](items: Ex[ISeq[A]]): ComboBox[A] = Impl(items)

  private final class Expanded[S <: Sys[S], A](protected val peer: ComboBox[A]) extends View[S]
    with ComponentHolder[de.sciss.swingplus.ComboBox[A]] with ComponentExpandedImpl[S] {

    type C = de.sciss.swingplus.ComboBox[A]

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val index   = ctx.getProperty[Ex[Int]]      (peer, keyIndex      ).fold(-1) (_.expand[S].value)
      val itemOpt = ctx.getProperty[Ex[Option[A]]](peer, keyValueOption).flatMap  (_.expand[S].value)
      val items0  = peer.items.expand[S].value
      deferTx {
        val c = new de.sciss.swingplus.ComboBox[A](items0)
        if (index >= 0 && index < items0.size) c.selection.index = index
        itemOpt.foreach { item => c.selection.item = item }
        component = c
      }

      initProperty(keyIndex, 0)(component.selection.index = _)
      initProperty(keyValueOption, Option.empty[A])(opt => opt.foreach(component.selection.item = _))

      super.initComponent()
    }
  }

  private final class ValueOptionExpanded[S <: Sys[S], A](ws: View.T[S, de.sciss.swingplus.ComboBox[A]], value0: Option[A])
                                                         (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Option[A]]
      with IGenerator[S, Change[Option[A]]] {

    private def commit(): Unit = {
      val c       = ws.component
      val before  = guiValue
      val now     = Option(c.selection.item)
      val ch      = Change(before, now)
      if (ch.isSignificant) {
        guiValue    = now
        cursor.step { implicit tx =>
          txValue.set(now)(tx.peer)
          fire(ch)
        }
      }
    }

    private[this] var guiValue: Option[A] = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): Option[A] = txValue.get(tx.peer)

    def changed: IEvent[S, Change[Option[A]]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Option[A]]] =
      Some(pull.resolve[Change[Option[A]]])

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        // N.B.: it is ok to register the same reactor twice,
        // each listener will still be notified only once.
        // (if we used a new `Reactions` instance, we would need
        // to ensure it doesn't get garbage-collected!)
        c.listenTo(c.selection)
        c.reactions += {
          case SelectionChanged(_) => commit()
        }
        guiValue = Option(c.selection.item)
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val c = ws.component
        // N.B.: this will stop delivering events to _any_ listener,
        // however `dispose()` will be called for the entire graph,
        // so that is not a problem
        c.deafTo(c.selection)
      }
    }
  }

  private[graph] final val keyIndex          = "index"
  private[graph] final val keyValueOption    = "valueOption"

  final case class Index[A](w: ComboBox[A]) extends Ex[Int] {
    override def productPrefix: String = s"ComboBox$$Index" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Int] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val indexOpt  = ctx.getProperty[Ex[Int]](w, keyIndex)
      val index0    = indexOpt.fold[Int]({
        val vec = w.items.expand[S].value
        if (vec.isEmpty) -1 else 0
      })(_.expand[S].value)
      new ComboBoxIndexExpandedImpl[S, A](ws.component, index0).init()
    }
  }

  final case class ValueOption[A](w: ComboBox[A]) extends Ex[Option[A]] {
    override def productPrefix: String = s"ComboBox$$ValueOption" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Option[A]] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[S]
      val itemOpt   = ctx.getProperty[Ex[Option[A]]](w, keyValueOption)
      val item0     = itemOpt.fold[Option[A]]({
        val vec = w.items.expand[S].value
        vec.headOption
      })(_.expand[S].value)
      new ValueOptionExpanded[S, A](ws, item0).init()
    }
  }

  private final case class Impl[A](items: Ex[ISeq[A]]) extends ComboBox[A] with ComponentImpl { w =>
    override def productPrefix = "ComboBox"   // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S, A](this).initComponent()

    object index extends Model[Int] {
      def apply(): Ex[Int] = Index(w)

      def update(value: Ex[Int]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyIndex, value)
      }
    }

    object valueOption extends Model[Option[A]] {
      def apply(): Ex[Option[A]] = ValueOption(w)

      def update(value: Ex[Option[A]]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValueOption, value)
      }
    }
  }
}
trait ComboBox[A] extends Component {
  type C = de.sciss.swingplus.ComboBox[A]

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  def items: Ex[ISeq[A]]

  /** Index of selected item or `-1` */
  def index: Model[Int]

  /** Some selected item or `None` */
  def valueOption: Model[Option[A]]
}