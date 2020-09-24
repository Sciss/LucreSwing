/*
 *  ComboBox.scala
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

import de.sciss.lucre.impl.IChangeGeneratorEvent
import de.sciss.lucre.{Cursor, IChangeEvent, IExpr, IPull, ITargets, Txn}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComboBoxIndexExpandedImpl, ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref
import scala.swing.event.SelectionChanged

// Note: we use SwingPlus because it allows us to use model instead of static seq
// which may come handy at some point
object ComboBox {

  def apply[A](items: Ex[Seq[A]]): ComboBox[A] = Impl(items)

  private final class Expanded[T <: Txn[T], A](protected val peer: ComboBox[A]) extends View[T]
    with ComponentHolder[de.sciss.swingplus.ComboBox[A]] with ComponentExpandedImpl[T] {

    type C = de.sciss.swingplus.ComboBox[A]

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val index   = ctx.getProperty[Ex[Int]]      (peer, keyIndex      ).fold(-1) (_.expand[T].value)
      val itemOpt = ctx.getProperty[Ex[Option[A]]](peer, keyValueOption).flatMap  (_.expand[T].value)
      val items0  = peer.items.expand[T].value
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

  private final class ValueOptionExpanded[T <: Txn[T], A](ws: View.T[T, de.sciss.swingplus.ComboBox[A]], value0: Option[A])
                                                         (implicit protected val targets: ITargets[T], cursor: Cursor[T])
    extends IExpr[T, Option[A]]
      with IChangeGeneratorEvent[T, Option[A]] {

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

    def value(implicit tx: T): Option[A] = txValue.get(tx.peer)

    def changed: IChangeEvent[T, Option[A]] = this

    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): Option[A] =
      pull.resolveExpr(this)

    def init()(implicit tx: T): this.type = {
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

    def dispose()(implicit tx: T): Unit = {
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
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"ComboBox$$Index" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[T]
      val indexOpt  = ctx.getProperty[Ex[Int]](w, keyIndex)
      val index0    = indexOpt.fold[Int]({
        val vec = w.items.expand[T].value
        if (vec.isEmpty) -1 else 0
      })(_.expand[T].value)
      new ComboBoxIndexExpandedImpl[T, A](ws.component, index0).init()  // IntelliJ highlight bug
    }
  }

  final case class ValueOption[A](w: ComboBox[A]) extends Ex[Option[A]] {
    type Repr[T <: Txn[T]] = IExpr[T, Option[A]]

    override def productPrefix: String = s"ComboBox$$ValueOption" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      val ws        = w.expand[T]
      val itemOpt   = ctx.getProperty[Ex[Option[A]]](w, keyValueOption)
      val item0     = itemOpt.fold[Option[A]]({
        val vec = w.items.expand[T].value
        vec.headOption
      })(_.expand[T].value)
      new ValueOptionExpanded[T, A](ws, item0).init()
    }
  }

  private final case class Impl[A](items: Ex[Seq[A]]) extends ComboBox[A] with ComponentImpl { w =>
    override def productPrefix = "ComboBox"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T, A](this).initComponent()

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

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  def items: Ex[Seq[A]]

  /** Index of selected item or `-1` */
  def index: Model[Int]

  /** Some selected item or `None` */
  def valueOption: Model[Option[A]]
}