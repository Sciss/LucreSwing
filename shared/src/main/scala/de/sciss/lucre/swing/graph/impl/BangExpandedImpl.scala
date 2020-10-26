/*
 *  BangExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.IPush.Parents
import de.sciss.lucre.Txn.{peer => txPeer}
import de.sciss.lucre.expr.graph.Trig
import de.sciss.lucre.expr.{IAction, ITrigger}
import de.sciss.lucre.impl.IGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.{Cursor, Disposable, IEvent, IPull, ITargets, Txn}

import scala.concurrent.stm.Ref

final class BangExpandedImpl[T <: Txn[T]](protected val peer: Bang)(implicit protected val targets: ITargets[T],
                                                                    protected val cursor: Cursor[T])
  extends View[T]
    with BangExpandedPlatform[T]
    with IAction[T] with ITrigger[T]
    with IGeneratorEvent[T, Unit] {

  override def toString: String = s"Bang.Expanded@${hashCode().toHexString}"

//  type C = View.Component // scala.swing.Button

  private[this] val disposables = Ref(List.empty[Disposable[T]])

  private def addDisposable(d: Disposable[T])(implicit tx: T): Unit =
    disposables.transform(d :: _)

  def executeAction()(implicit tx: T): Unit = {
    fire(())
    activate()
  }

  def addSource(tr: ITrigger[T])(implicit tx: T): Unit = {
    // ok, this is a bit involved:
    // we must either mixin the trait `Caching` or
    // create an observer to not be eliminated from event
    // reaction execution. If we don't do that, we'd only
    // see activation when our trigger output is otherwise
    // observed (e.g. goes into a `PrintLn`).
    // What we do here is, first, wire the two events together,
    // so that any instancing checking our trigger will observe it
    // within the same event loop as the input trigger, and second,
    // have the observation side effect (`activate`).
    tr.changed ---> changed
    val obs = tr.changed.react { implicit tx => _ => activate() }
    addDisposable(obs)
  }

  def changed: IEvent[T, Unit] = this

  private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T) : Option[Unit] = {
    if (pull.isOrigin(this)) Trig.Some
    else {
      val p: Parents[T] = pull.parents(this)
      if (p.exists(pull(_).isDefined)) Trig.Some else None
    }
  }

  protected def bang(): Unit =
    cursor.step { implicit tx =>
      executeAction()
    }

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()
    disposables.swap(Nil).foreach(_.dispose())
    deferTx {
      guiDispose()
    }
  }
}
