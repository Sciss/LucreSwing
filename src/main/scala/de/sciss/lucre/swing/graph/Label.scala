/*
 *  Label.scala
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

import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.{Swing, Label => Peer}

object Label {
  def apply(text: Ex[String]): Label = Impl(text)

  private final class Expanded[S <: Sys[S]](protected val w: Label) extends View[S]
    with ComponentHolder[Peer] with ComponentExpandedImpl[S] {

    type C = Peer

    private[this] var obs: Disposable[S#Tx] = _

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val text    = w.text.expand[S]
      val text0   = text.value
      val hAlign  = ctx.getProperty[Ex[Int]](w, keyHAlign).fold(defaultHAlign)(_.expand[S].value)
      val vAlign  = ctx.getProperty[Ex[Int]](w, keyVAlign).fold(defaultVAlign)(_.expand[S].value)

      deferTx {
        val hAlignSwing = hAlign match {
          case Align.Left     => scala.swing.Alignment.Left
          case Align.Center   => scala.swing.Alignment.Center
          case Align.Right    => scala.swing.Alignment.Right
          case Align.Trailing => scala.swing.Alignment.Trailing
          case _              => scala.swing.Alignment.Leading
        }
        // N.B. Scala Swing uses divergent default horizontal alignment of Center instead of Java Swing (CENTER)
        val c = new Peer(text0, Swing.EmptyIcon, hAlignSwing)
        if (vAlign != defaultVAlign) {
          c.verticalAlignment = vAlign match {
            case Align.Top      => scala.swing.Alignment.Top
            case Align.Bottom   => scala.swing.Alignment.Bottom
            case _              => scala.swing.Alignment.Center
          }
        }
        component = c
      }
      obs = text.changed.react { implicit tx => ch =>
        deferTx {
          component.text = ch.now
        }
      }
      super.init()
    }

    override def dispose()(implicit tx: S#Tx): Unit = {
      obs.dispose()
      super.dispose()
    }
  }

  final case class HAlign(w: Component) extends Ex[Int] {
    override def productPrefix: String = s"Label$$HAlign" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHAlign)
      valueOpt.fold(Constant(defaultHAlign).expand[S])(_.expand[S])
    }
  }

  final case class VAlign(w: Component) extends Ex[Int] {
    override def productPrefix: String = s"Label$$VAlign" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVAlign)
      valueOpt.fold(Constant(defaultVAlign).expand[S])(_.expand[S])
    }
  }

  private final case class Impl(text0: Ex[String]) extends Label with ComponentImpl {
    override def productPrefix: String = "Label" // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

    def text: Ex[String] = text0

    def hAlign: Ex[Int] = Label.HAlign(this)

    def hAlign_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyHAlign, value)
    }

    def vAlign: Ex[Int] = Label.VAlign(this)

    def vAlign_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyVAlign, value)
    }
  }

  private final val keyHAlign          = "hAlign"
  private final val keyVAlign          = "vAlign"
  private def       defaultHAlign: Int = Align.Leading
  private def       defaultVAlign: Int = Align.Center
}
trait Label extends Component {
  type C = Peer

  /** The label's text */
  def text: Ex[String]

  /** Horizontal alignment:
    * The alignment must be one of `Align.Left`, `Align.Center`, `Align.Right`, `Align.Leading`, `Align.Trailing`.
    * Setting an invalid value makes the component default aligned (`Leading`).
    */
  var hAlign: Ex[Int]

  /** Vertical alignment:
    * The alignment must be one of `Align.Top`, `Align.Center`, `Align.Bottom`.
    * Setting an invalid value makes the component default aligned (`Center`).
    */
  var vAlign: Ex[Int]
}