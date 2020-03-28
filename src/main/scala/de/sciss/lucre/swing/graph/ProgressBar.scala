/*
 *  ProgressBar.scala
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

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object ProgressBar {
  def apply(): ProgressBar = Impl()

  private final class Expanded[S <: Sys[S]](protected val peer: ProgressBar) extends View[S]
    with ComponentHolder[scala.swing.ProgressBar] with ComponentExpandedImpl[S] {

    type C = scala.swing.ProgressBar

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      deferTx {
        val c = new scala.swing.ProgressBar
        component = c
      }
      initProperty(keyMin         , defaultMin          )(component.min           = _)
      initProperty(keyMax         , defaultMax          )(component.max           = _)
      initProperty(keyValue       , defaultMin          )(component.value         = _)
      initProperty(keyLabel       , defaultLabel        )(component.label         = _)
      initProperty(keyLabelPainted, defaultLabelPainted )(component.labelPainted  = _)

      super.initComponent()
    }
  }

  private final val keyValue            = "value"
  private final val keyMin              = "min"
  private final val keyMax              = "max"
  private final val keyLabel            = "label"
  private final val keyLabelPainted     = "labelPainted"

  private final val defaultMin          =   0
  private final val defaultMax          = 100
  private final val defaultLabel        = ""
  private final val defaultLabelPainted = false

  final case class Value(w: ProgressBar) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"ProgressBar$$Value" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt  = ctx.getProperty[Ex[Int]](w, keyValue)
      valueOpt.orElse(ctx.getProperty[Ex[Int]](w, keyMin)).getOrElse(Const(defaultMin)).expand[S]
    }
  }

  final case class Min(w: ProgressBar) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"ProgressBar$$Min" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMin)
      valueOpt.getOrElse(Const(defaultMin)).expand[S]
    }
  }

  final case class Max(w: ProgressBar) extends Ex[Int] {
    type Repr[S <: Sys[S]] = IExpr[S, Int]

    override def productPrefix: String = s"ProgressBar$$Max" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMax)
      valueOpt.getOrElse(Const(defaultMax)).expand[S]
    }
  }

  final case class Label(w: ProgressBar) extends Ex[String] {
    type Repr[S <: Sys[S]] = IExpr[S, String]

    override def productPrefix: String = s"ProgressBar$$String" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyLabel)
      valueOpt.getOrElse(Const(defaultLabel)).expand[S]
    }
  }

  final case class LabelPainted(w: ProgressBar) extends Ex[Boolean] {
    type Repr[S <: Sys[S]] = IExpr[S, Boolean]

    override def productPrefix: String = s"ProgressBar$$LabelPainted" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyLabelPainted)
      valueOpt.getOrElse(Const(defaultLabelPainted)).expand[S]
    }
  }

  private final case class Impl() extends ProgressBar with ComponentImpl {
    override def productPrefix = "ProgressBar"   // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

    def min: Ex[Int] = Min(this)

    def min_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMin, x)
    }

    def max: Ex[Int] = Max(this)

    def max_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMax, x)
    }

    def value: Ex[Int] = Value(this)

    def value_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyValue, x)
    }

    def label: Ex[String] = ProgressBar.Label(this)

    def label_=(x: Ex[String]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyLabel, x)
    }

    def labelPainted: Ex[Boolean] = LabelPainted(this)

    def labelPainted_=(x: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyLabelPainted, x)
    }
  }
}
trait ProgressBar extends Component {
  type C = scala.swing.ProgressBar

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  var min         : Ex[Int]
  var max         : Ex[Int]
  var value       : Ex[Int]

  var label       : Ex[String]
  var labelPainted: Ex[Boolean]
}