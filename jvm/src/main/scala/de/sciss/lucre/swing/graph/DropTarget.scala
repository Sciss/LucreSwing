/*
 *  DropTarget.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import java.awt.Dimension
import java.awt.datatransfer.{DataFlavor, Transferable}

import de.sciss.lucre.Txn.peer
import de.sciss.lucre.expr.graph.{Control, Ex, Trig}
import de.sciss.lucre.expr.impl.IControlImpl
import de.sciss.lucre.expr.{Context, IControl, ITrigger}
import de.sciss.lucre.impl.{IChangeEventImpl, IEventImpl, IGeneratorEvent}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{TargetIcon, View}
import de.sciss.lucre.{Adjunct, Caching, Cursor, IChangeEvent, IEvent, IExpr, IPublisher, IPull, IPush, ITargets, ProductWithAdjuncts, Txn}
import de.sciss.model.Change
import de.sciss.serial.DataInput
import javax.swing.TransferHandler.TransferSupport
import javax.swing.{JLabel, TransferHandler}

import scala.concurrent.stm.Ref
import scala.swing.Graphics2D
import scala.util.control.NonFatal

object DropTarget {
  def apply(): DropTarget = Impl()

  private lazy val _init: Unit = {
    Adjunct.addFactory(Selector.String)
    Adjunct.addFactory(Selector.File  )
  }

  def init(): Unit = _init

  object Selector {
    implicit object String extends Selector[java.lang.String] with Adjunct.Factory {
      final val id = 3000

      def canImport[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): Boolean =
        t.isDataFlavorSupported(DataFlavor.stringFlavor)

      def defaultData: String = ""

      def importData[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): String =
        t.getTransferData(DataFlavor.stringFlavor).asInstanceOf[java.lang.String]

      def readIdentifiedAdjunct(in: DataInput): Adjunct = this
    }

    implicit object File extends Selector[java.io.File] with Adjunct.Factory {
      final val id = 3001

      def canImport[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): Boolean =
        t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

      def defaultData: java.io.File = new java.io.File("")

      def importData[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): java.io.File = {
        val data = t.getTransferData(DataFlavor.javaFileListFlavor).asInstanceOf[java.util.List[java.io.File]]
        data.get(0)
      }

      def readIdentifiedAdjunct(in: DataInput): Adjunct = this
    }
  }
  trait Selector[+A] extends Adjunct {
    def canImport[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): Boolean

    def defaultData: A

    /** May throw an exception. */
    def importData[T <: Txn[T]](t: Transferable)(implicit ctx: Context[T]): A
  }

  private final class ValueExpanded[T <: Txn[T], A](value0: A, evt: IEvent[T, A], tx0: T)
                                                   (implicit protected val targets: ITargets[T])
    extends IExpr[T, A] with IChangeEventImpl[T, A] with Caching {

    private[this] val ref = Ref(value0) // requires caching!

    evt.--->(this)(tx0)

    def value(implicit tx: T): A =
      IPush.tryPull(this).fold(ref())(_.now)

    def dispose()(implicit tx: T): Unit =
      evt.-/->(this)

//    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): A = {
//      val v = pull.applyChange(evt)
//    }

    private[lucre] def pullChange(pull: IPull[T])(implicit tx: T, phase: IPull.Phase): A =
      if (phase.isBefore) ref() else pull(evt).get

    override private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[Change[A]] =
      pull(evt).flatMap { aNow =>
        val aBefore = ref()
        if (aBefore != aNow) {
          ref() = aNow
          val ch = Change(aBefore, aNow)
          Some(ch)
        } else {
          None
        }
      }

    def changed: IChangeEvent[T, A] = this
  }

  private final class ReceivedExpanded[T <: Txn[T]](evt: IEvent[T, Any], tx0: T)
                                                   (implicit protected val targets: ITargets[T])
    extends ITrigger[T] with IEventImpl[T, Unit] {

    evt.--->(this)(tx0)

    def dispose()(implicit tx: T): Unit =
      evt.-/->(this)

    private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[Unit] = Trig.Some

    def changed: IEvent[T, Unit] = this
  }

  final case class Value[A](s: Select[A]) extends Ex[A] {
    type Repr[T <: Txn[T]] = IExpr[T, A]

    override def productPrefix = s"DropTarget$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val sEx = s.expand[T]
      import ctx.targets
      new ValueExpanded(s.selector.defaultData, sEx.changed, tx)
    }
  }

  final case class Received[A](s: Select[A]) extends Trig {
    type Repr[T <: Txn[T]] = ITrigger[T]

    override def productPrefix = s"DropTarget$$Received" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val sEx = s.expand[T]
      import ctx.targets
      new ReceivedExpanded(sEx.changed, tx)
    }
  }

  private final class SelectExpanded[T <: Txn[T], A](protected val peer: Select[A], repr: Repr[T])
                                                    (implicit protected val targets: ITargets[T],
                                                     cursor: Cursor[T])
    extends IControlImpl[T] with IGeneratorEvent[T, A] with IPublisher[T, A] {

    private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T): Option[A] =
      Some(pull.resolve)

    def initSelect()(implicit tx: T): this.type = {
      deferTx {
        val p: Peer = repr.component    // IntelliJ highlight bug
        p.addSelector(peer.selector) { value =>
          cursor.step { implicit tx =>
            fire(value)
          }
        }
      }
      this
    }

    def changed: IEvent[T, A] = this
  }

  final case class Select[A](w: DropTarget)(implicit val selector: Selector[A])
    extends Control with ProductWithAdjuncts {

    type Repr[T <: Txn[T]] = IControl[T] with IPublisher[T, A]

    override def productPrefix = s"DropTarget$$Select" // serialization

    def adjuncts: List[Adjunct] = selector :: Nil

    def received: Trig  = Received(this)
    def value   : Ex[A] = Value   (this)

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      new SelectExpanded(this, w.expand[T]).initSelect()
    }
  }

  private final case class DropEvent[A](s: Selector[A])

  private final class PeerImpl[T <: Txn[T]](implicit ctx: Context[T]) extends Peer {
    private[this] var selectors = List.empty[SelectorFun[_]]

    override lazy val peer: JLabel = new JLabel(" ") with SuperMixin {
      // XXX TODO --- hack to avoid too narrow buttons under certain look-and-feel
      override def getPreferredSize: Dimension = {
        val d = super.getPreferredSize
        if (!isPreferredSizeSet) {
          val e     = math.max(24, math.max(d.width, d.height))
          d.width   = e
          d.height  = e
        }
        d
      }
    }

    override protected def paintComponent(g: Graphics2D): Unit = {
      super.paintComponent(g)
      val p       = peer
      val w       = p.getWidth
      val h       = p.getHeight
      val extent  = math.min(w, h)
      if (extent > 0) {
        TargetIcon.paint(g, x = (w - extent) >> 1, y = (h - extent) >> 1, extent = extent, enabled = enabled)
      }
    }

    private object TH extends TransferHandler {
      override def canImport (support: TransferSupport): Boolean = enabled && {
        val t   = support.getTransferable
        val res = selectors.exists(_.s.canImport(t))
        res
      }

      override def importData(support: TransferSupport): Boolean = enabled && {
        val t   = support.getTransferable
        val res = selectors.find(_.s.canImport(t))
        res.exists(_.perform(t))
      }
    }

    peer.setTransferHandler(TH)

    private final class SelectorFun[A](val s: Selector[A], val done: A => Unit) {
      def perform(t: Transferable): Boolean =
        try {
          val value = s.importData(t)
          done(value)
          true
        } catch {
          case NonFatal(ex) =>
            ex.printStackTrace()
            false
        }
    }

    def addSelector[A](s: Selector[A])(done: A => Unit): Unit =
      selectors :+= new SelectorFun(s, done) // early selectors should come early
  }

  private final class Expanded[T <: Txn[T]](protected val peer: DropTarget) extends View[T]
    with ComponentHolder[Peer] with ComponentExpandedImpl[T] {

    type C = Peer

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      deferTx {
        val c = new PeerImpl
        component = c
      }
      super.initComponent()
    }
  }

  private final case class Impl() extends DropTarget with ComponentImpl {
    override def productPrefix = "DropTarget" // serialization

    def select[A: Selector]: Select[A] = Select(this)

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): DropTarget.Repr[T] =
      new Expanded[T](this).initComponent()
  }

  abstract class Peer extends scala.swing.Label {
    def addSelector[A](s: Selector[A])(done: A => Unit): Unit
  }

  type Repr[T <: Txn[T]] = View.T[T, Peer] with IControl[T]
}
trait DropTarget extends Component {
  type C = DropTarget.Peer

  type Repr[T <: Txn[T]] = DropTarget.Repr[T]

  def select[A: DropTarget.Selector]: DropTarget.Select[A]
}