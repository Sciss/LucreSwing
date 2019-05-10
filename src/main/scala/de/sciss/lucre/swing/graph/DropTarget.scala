/*
 *  DropTarget.scala
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

import java.awt.datatransfer.{DataFlavor, Transferable}

import de.sciss.lucre.aux.{Aux, ProductWithAux}
import de.sciss.lucre.event.impl.{IEventImpl, IGenerator}
import de.sciss.lucre.event.{Caching, IEvent, IPublisher, IPull, ITargets}
import de.sciss.lucre.expr.graph.{Control, Ex, Trig}
import de.sciss.lucre.expr.impl.IControlImpl
import de.sciss.lucre.expr.{Context, IControl, IExpr, ITrigger}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.swing.{TargetIcon, View, deferTx}
import de.sciss.model.Change
import de.sciss.serial.DataInput
import javax.swing.TransferHandler
import javax.swing.TransferHandler.TransferSupport

import scala.concurrent.stm.Ref
import scala.util.control.NonFatal

object DropTarget {
  def apply(): DropTarget = Impl()

  private lazy val _init: Unit = {
    Aux.addFactory(Selector.String)
    Aux.addFactory(Selector.File  )
  }

  def init(): Unit = _init

  object Selector {
    implicit object String extends Selector[java.lang.String] with Aux.Factory {
      final val id = 3000

      def canImport[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): Boolean =
        t.isDataFlavorSupported(DataFlavor.stringFlavor)

      def defaultData: String = ""

      def importData[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): String =
        t.getTransferData(DataFlavor.stringFlavor).asInstanceOf[java.lang.String]

      def readIdentifiedAux(in: DataInput): Aux = this
    }

    implicit object File extends Selector[java.io.File] with Aux.Factory {
      final val id = 3001

      def canImport[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): Boolean =
        t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)

      def defaultData: java.io.File = new java.io.File("")

      def importData[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): java.io.File = {
        val data = t.getTransferData(DataFlavor.javaFileListFlavor).asInstanceOf[java.util.List[java.io.File]]
        data.get(0)
      }

      def readIdentifiedAux(in: DataInput): Aux = this
    }
  }
  trait Selector[+A] extends Aux {
    def canImport[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): Boolean

    def defaultData: A

    /** May throw an exception. */
    def importData[S <: Sys[S]](t: Transferable)(implicit ctx: Context[S]): A
  }

  private final class ValueExpanded[S <: Sys[S], A](value0: A, evt: IEvent[S, A], tx0: S#Tx)
                                                   (implicit protected val targets: ITargets[S])
    extends IExpr[S, A] with IEventImpl[S, Change[A]] with Caching {

    private[this] val ref = Ref(value0) // requires caching!

    evt.--->(this)(tx0)

    def value(implicit tx: S#Tx): A = ref()

    def dispose()(implicit tx: S#Tx): Unit =
      evt.-/->(this)

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[A]] =
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

    def changed: IEvent[S, Change[A]] = this
  }

  private final class ReceivedExpanded[S <: Sys[S]](evt: IEvent[S, Any], tx0: S#Tx)
                                                   (implicit protected val targets: ITargets[S])
    extends ITrigger[S] with IEventImpl[S, Unit] {

    evt.--->(this)(tx0)

    def dispose()(implicit tx: S#Tx): Unit =
      evt.-/->(this)

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Unit] = Trig.Some

    def changed: IEvent[S, Unit] = this
  }

  final case class Value[A](s: Select[A]) extends Ex[A] {
    type Repr[S <: Sys[S]] = IExpr[S, A]

    override def productPrefix = s"DropTarget$$Value" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val sEx = s.expand[S]
      import ctx.targets
      new ValueExpanded(s.selector.defaultData, sEx.changed, tx)
    }
  }

  final case class Received[A](s: Select[A]) extends Trig {
    type Repr[S <: Sys[S]] = ITrigger[S]

    override def productPrefix = s"DropTarget$$Received" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val sEx = s.expand[S]
      import ctx.targets
      new ReceivedExpanded(sEx.changed, tx)
    }
  }

  private final class SelectExpanded[S <: Sys[S], A](protected val peer: Select[A], repr: Repr[S])
                                                    (implicit protected val targets: ITargets[S],
                                                     cursor: stm.Cursor[S])
    extends IControlImpl[S] with IGenerator[S, A] with IPublisher[S, A] {

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[A] =
      Some(pull.resolve[A])

    def initSelect()(implicit tx: S#Tx): this.type = {
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

    def changed: IEvent[S, A] = this
  }

  final case class Select[A](w: DropTarget)(implicit val selector: Selector[A])
    extends Control with ProductWithAux {

    type Repr[S <: Sys[S]] = IControl[S] with IPublisher[S, A]

    override def productPrefix = s"DropTarget$$Select" // serialization

    def aux: List[Aux] = selector :: Nil

    def received: Trig  = Received(this)
    def value   : Ex[A] = Value   (this)

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      import ctx.{cursor, targets}
      new SelectExpanded(this, w.expand[S]).initSelect()
    }
  }

  private final case class DropEvent[A](s: Selector[A])

  private final class PeerImpl[S <: Sys[S]](implicit ctx: Context[S]) extends Peer {
    private[this] var selectors = List.empty[SelectorFun[_]]

    icon          = new TargetIcon()
    disabledIcon  = new TargetIcon(enabled = false)

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
          case NonFatal(_) =>
            false
        }
    }

    def addSelector[A](s: Selector[A])(done: A => Unit): Unit =
      selectors :+= new SelectorFun(s, done) // early selectors should come early
  }

  private final class Expanded[S <: Sys[S]](protected val peer: DropTarget) extends View[S]
    with ComponentHolder[Peer] with ComponentExpandedImpl[S] {

    type C = Peer

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
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

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()
  }

  abstract class Peer extends scala.swing.Label {
    def addSelector[A](s: Selector[A])(done: A => Unit): Unit
  }

  type Repr[S <: Sys[S]] = View.T[S, Peer] with IControl[S]
}
trait DropTarget extends Component {
  type C = DropTarget.Peer

  type Repr[S <: Sys[S]] = DropTarget.Repr[S]

  def select[A: DropTarget.Selector]: DropTarget.Select[A]
}