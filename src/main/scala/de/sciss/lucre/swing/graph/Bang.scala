/*
 *  Bang.scala
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

import java.awt.Dimension
import java.awt.event.{ActionEvent, ActionListener}
import java.awt.geom.Ellipse2D

import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.Ex.Context
import de.sciss.lucre.expr.{Act, IAction, IControl, ITrigger, Trig}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.stm.TxnLike.{peer => txPeer}
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import javax.swing.{JButton, Timer}

import scala.concurrent.stm.Ref
import scala.swing.Graphics2D
import scala.swing.event.ButtonClicked

object Bang {
  def apply(): Bang = Impl()

  private final class Expanded[S <: Sys[S]](protected val peer: Bang)(implicit protected val targets: ITargets[S],
                                                                      cursor: stm.Cursor[S])
    extends View[S]
    with ComponentHolder[scala.swing.Button] with ComponentExpandedImpl[S]
    with IAction[S] with ITrigger[S]
    with IGenerator[S, Unit] {

    type C = scala.swing.Button

    private[this] val disposables = Ref(List.empty[Disposable[S#Tx]])

    private def addDisposable(d: Disposable[S#Tx])(implicit tx: S#Tx): Unit =
      disposables.transform(d :: _)

    def executeAction()(implicit tx: S#Tx): Unit = {
      fire(())
      activate()
    }

    def addSource(tr: ITrigger[S])(implicit tx: S#Tx): Unit = {
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

    private def activate()(implicit tx: S#Tx): Unit = {
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

    def changed: IEvent[S, Unit] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx) : Option[Unit] = Trig.Some

    private[this] var active = false

    private[this] var timer: Timer = _

    private def bang(): Unit =
      cursor.step { implicit tx =>
        executeAction()
      }

    override def dispose()(implicit tx: S#Tx): Unit = {
      super.dispose()
      disposables.swap(Nil).foreach(_.dispose())
      deferTx {
        timer.stop()
      }
    }

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
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

    protected def mkControl[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      import ctx.{cursor, targets}
      new Expanded[S](this).initComponent()
    }
  }
}
trait Bang extends Component with Act with Trig {
  final type C = scala.swing.Button

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S] with ITrigger[S] with IAction[S]
}