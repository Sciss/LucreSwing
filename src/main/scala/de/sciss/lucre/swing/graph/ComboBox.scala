/*
 *  ComboBox.scala
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

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.Widget.Model
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.collection.immutable.{Seq => ISeq}
import scala.concurrent.stm.Ref
import scala.swing.event.SelectionChanged

// Note: we use SwingPlus because it allows us to use model instead of static seq
// which may come handy at some point
object ComboBox {

  def apply[A](items: Ex[ISeq[A]]): ComboBox[A] = Impl(items)

  private final class Expanded[S <: Sys[S], A](protected val w: ComboBox[A]) extends View[S]
    with ComponentHolder[de.sciss.swingplus.ComboBox[A]] with ComponentExpandedImpl[S] {

    type C = de.sciss.swingplus.ComboBox[A]

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val index   = b.getProperty[Ex[Int]]      (w, keyIndex).fold(-1)(_.expand[S].value)
      val itemOpt = b.getProperty[Ex[Option[A]]](w, keyValueOption).flatMap(_.expand[S].value)
      val items0  = w.items.expand[S].value
      deferTx {
        val c = new de.sciss.swingplus.ComboBox[A](items0)
        if (index >= 0 && index < items0.size) c.selection.index = index
        itemOpt.foreach { item => c.selection.item = item }
        component = c
      }
      super.init()
    }
  }

  private final class IndexExpanded[S <: Sys[S], A](ws: View.T[S, de.sciss.swingplus.ComboBox[A]], value0: Int)
                                                   (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, Int]
      with IGenerator[S, Change[Int]] {

    private def commit(): Unit = {
      val c       = ws.component
      val before  = guiValue
      val now     = c.selection.index
      val ch      = Change(before, now)
      if (ch.isSignificant) {
        guiValue    = now
        cursor.step { implicit tx =>
          txValue.set(now)(tx.peer)
          fire(ch)
        }
      }
    }

    private[this] var guiValue: Int = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): Int = txValue.get(tx.peer)

    def changed: IEvent[S, Change[Int]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Int]] =
      Some(pull.resolve[Change[Int]])

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
        guiValue = c.selection.index
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

  private final val keyIndex          = "index"
  private final val keyValueOption    = "valueOption"

  final case class Index[A](w: ComboBox[A]) extends Ex[Int] {
    override def productPrefix: String = s"ComboBox$$Index" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        import b.{cursor, targets}
        val ws        = w.expand[S](b, tx)
        val indexOpt  = b.getProperty[Ex[Int]](w, keyIndex)
        val index0    = indexOpt.fold[Int]({
          val vec = w.items.expand[S].value
          if (vec.isEmpty) -1 else 0
        })(_.expand[S].value)
        new IndexExpanded[S, A](ws, index0).init()
    }

    def aux: List[Aux] = Nil
  }

  final case class ValueOption[A](w: ComboBox[A]) extends Ex[Option[A]] {
    override def productPrefix: String = s"ComboBox$$ValueOption" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Option[A]] = ctx match {
      case b: Widget.Builder[S] =>
        import b.{cursor, targets}
        val ws        = w.expand[S](b, tx)
        val itemOpt   = b.getProperty[Ex[Option[A]]](w, keyValueOption)
        val item0     = itemOpt.fold[Option[A]]({
          val vec = w.items.expand[S].value
          vec.headOption
        })(_.expand[S].value)
        new ValueOptionExpanded[S, A](ws, item0).init()
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl[A](items: Ex[ISeq[A]]) extends ComboBox[A] with ComponentImpl { w =>
    override def productPrefix = "ComboBox"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
      new Expanded[S, A](this).init()

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

  def items: Ex[ISeq[A]]

  /** Index of selected item or `-1` */
  def index: Model[Int]

  /** Some selected item or `None` */
  def valueOption: Model[Option[A]]
}