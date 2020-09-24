/*
 *  Bang.scala
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

import java.awt.Dimension
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.geom.Ellipse2D

import de.sciss.lucre.IPush.Parents
import de.sciss.lucre.Txn.{peer => txPeer}
import de.sciss.lucre.expr.graph.{Act, Trig}
import de.sciss.lucre.expr.{Context, IAction, IControl, ITrigger}
import de.sciss.lucre.impl.IGeneratorEvent
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Cursor, Disposable, IEvent, IPull, ITargets, Txn}
import javax.swing.{JButton, Timer}

import scala.concurrent.stm.Ref
import scala.swing.Graphics2D
import scala.swing.event.ButtonClicked

object Bang {
  def apply(): Bang = Impl()

  private final class Expanded[T <: Txn[T]](protected val peer: Bang)(implicit protected val targets: ITargets[T],
                                                                      cursor: Cursor[T])
    extends View[T]
    with ComponentHolder[scala.swing.Button] with ComponentExpandedImpl[T]
    with IAction[T] with ITrigger[T]
    with IGeneratorEvent[T, Unit] {

    override def toString: String = s"Bang.Expanded@${hashCode().toHexString}"

    type C = scala.swing.Button

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

    private def activate()(implicit tx: T): Unit = {
      deferTx {
        setActive(true)
        timer.restart()
      }
    }

    private def setActive(value: Boolean): Unit =
      if (active != value) {
        active = value
        val c = component
        c.repaint()
        // in Linux, repainting may be quite strongly delayed
        // if the mouse or keyboard is not active.
        // Thus force graphics state sync
        c.toolkit.sync()
      }

    def changed: IEvent[T, Unit] = this

    private[lucre] def pullUpdate(pull: IPull[T])(implicit tx: T) : Option[Unit] = {
      if (pull.isOrigin(this)) Trig.Some
      else {
        val p: Parents[T] = pull.parents(this)
        if (p.exists(pull(_).isDefined)) Trig.Some else None
      }
    }

    private[this] var active = false

    private[this] var timer: Timer = _

    private def bang(): Unit =
      cursor.step { implicit tx =>
        executeAction()
      }

    override def dispose()(implicit tx: T): Unit = {
      super.dispose()
      disposables.swap(Nil).foreach(_.dispose())
      deferTx {
        timer.stop()
      }
    }

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      deferTx {
        timer = new Timer(200, new ActionListener {
          def actionPerformed(e: ActionEvent): Unit =
            setActive(false)
        })
        timer.setRepeats(false)

        val c: C = new scala.swing.Button {
          private[this] val el = new Ellipse2D.Double()

          override lazy val peer: JButton = new JButton(" ") with SuperMixin {
            // XXX TODO --- hack to avoid too narrow buttons under certain look-and-feel
            override def getPreferredSize: Dimension = {
              val d = super.getPreferredSize
              if (!isPreferredSizeSet) {
                d.width = math.max(d.width, d.height)
              }
              d
            }
          }

          override protected def paintComponent(g: Graphics2D): Unit = {
            super.paintComponent(g)
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
              java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(java.awt.RenderingHints.KEY_STROKE_CONTROL,
              java.awt.RenderingHints.VALUE_STROKE_PURE)
            g.setColor(foreground)
            val p = peer
            val w = p.getWidth
            val h = p.getHeight
            val ext1 = math.max(0, math.min(w, h) - 14)
            val _el = el
            _el.setFrame((w - ext1) * 0.5, (h - ext1) * 0.5, ext1, ext1)
            g.draw(_el)
//            if (active) {
//              val ext2 = math.max(0, ext1 - 5)
//              _el.setFrame((w - ext2) * 0.5, (h - ext2) * 0.5, ext2, ext2)
//              g.fill(_el)
//            }
            if (active) g.fill(_el) else g.draw(_el)
          }

          reactions += {
            case ButtonClicked(_) => bang()
          }
        }
        component = c
      }
      super.initComponent()
    }
  }

  private final case class Impl() extends Bang with ComponentImpl {
    override def productPrefix = "Bang"   // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      import ctx.{cursor, targets}
      new Expanded[T](this).initComponent()
    }
  }
}
trait Bang extends Component with Act with Trig {
  final type C = scala.swing.Button

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T] with ITrigger[T] with IAction[T]
}