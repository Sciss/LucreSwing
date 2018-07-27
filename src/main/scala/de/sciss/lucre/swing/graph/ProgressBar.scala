/*
 *  ProgressBar.scala
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
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder

object ProgressBar {
  def apply(): ProgressBar = Impl()

  private final class Expanded[S <: Sys[S]](protected val w: ProgressBar) extends View[S]
    with ComponentHolder[scala.swing.ProgressBar] with ComponentExpandedImpl[S] {

    type C = scala.swing.ProgressBar

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val minOpt          = b.getProperty[Ex[Int    ]](w, keyMin          ).map(_.expand[S].value)
      val maxOpt          = b.getProperty[Ex[Int    ]](w, keyMax          ).map(_.expand[S].value)
      val valueOpt        = b.getProperty[Ex[Int    ]](w, keyValue        ).map(_.expand[S].value)
      val labelOpt        = b.getProperty[Ex[String ]](w, keyLabel        ).map(_.expand[S].value)
      val labelPaintedOpt = b.getProperty[Ex[Boolean]](w, keyLabelPainted ).map(_.expand[S].value)

      deferTx {
        val c = new scala.swing.ProgressBar
        minOpt          .foreach(c.min          = _)
        maxOpt          .foreach(c.max          = _)
        valueOpt        .foreach(c.value        = _)
        labelOpt        .foreach(c.label        = _)
        labelPaintedOpt .foreach(c.labelPainted = _)
        component = c
      }

      super.init()
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
    override def productPrefix: String = s"ProgressBar$$Value" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt  = b.getProperty[Ex[Int]](w, keyValue)
        valueOpt.orElse(b.getProperty[Ex[Int]](w, keyMin)).getOrElse(Constant(defaultMin)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Min(w: ProgressBar) extends Ex[Int] {
    override def productPrefix: String = s"ProgressBar$$Min" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, keyMin)
        valueOpt.getOrElse(Constant(defaultMin)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Max(w: ProgressBar) extends Ex[Int] {
    override def productPrefix: String = s"ProgressBar$$Max" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, keyMax)
        valueOpt.getOrElse(Constant(defaultMax)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Label(w: ProgressBar) extends Ex[String] {
    override def productPrefix: String = s"ProgressBar$$String" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, String] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[String]](w, keyLabel)
        valueOpt.getOrElse(Constant(defaultLabel)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class LabelPainted(w: ProgressBar) extends Ex[Boolean] {
    override def productPrefix: String = s"ProgressBar$$LabelPainted" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Boolean]](w, keyLabelPainted)
        valueOpt.getOrElse(Constant(defaultLabelPainted)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl() extends ProgressBar with ComponentImpl {
    override def productPrefix = "ProgressBar"   // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] =
      new Expanded[S](this).init()

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

  var min         : Ex[Int]
  var max         : Ex[Int]
  var value       : Ex[Int]

  var label       : Ex[String]
  var labelPainted: Ex[Boolean]
}