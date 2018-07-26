/*
 *  GridPanel.scala
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

object GridPanel {
  def apply(contents: Widget*): GridPanel = Impl(contents)

//  def mk(configure: GridPanel => Unit): GridPanel = {
//    val w = apply()
//    configure(w)
//    w
//  }
  
  private final val keyRows     = "rows"
  private final val keyColumns  = "columns"

  private final class Expanded[S <: Sys[S]](protected val w: GridPanel) extends View[S]
    with ComponentHolder[scala.swing.GridPanel] with ComponentExpandedImpl[S] {

    type C = scala.swing.GridPanel

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val rows0     = b.getProperty[Ex[Int]](w, keyRows    ).fold(defaultRows    )(_.expand[S].value)
      val columns   = b.getProperty[Ex[Int]](w, keyColumns ).fold(defaultColumns )(_.expand[S].value)
      val rows      = if (rows0 == 0 && columns == 0) 1 else 0  // not allowed to have both zero
      val contents  = w.contents.map(_.expand[S])
      deferTx {
        val vec = contents.map(_.component)
        val p   = new scala.swing.GridPanel(rows0 = rows, cols0 = columns)
        p.contents ++= vec
        component = p
      }
      super.init()
    }

//    def dispose()(implicit tx: S#Tx): Unit = super.dispose()
  }

  private final val defaultRows     = 0 // 1
  private final val defaultColumns  = 0

  final case class Rows(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Rows" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, keyRows)
        valueOpt.getOrElse(Constant(defaultRows)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Columns(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Columns" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, keyColumns)
        valueOpt.getOrElse(Constant(defaultColumns)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  private final case class Impl(contents: Seq[Widget]) extends GridPanel with ComponentImpl {
    override def productPrefix = "GridPanel" // s"GridPanel$$Impl" // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      new Expanded[S](this).init()
    }

    def rows: Ex[Int] = Rows(this)

    def rows_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyRows, x)
    }

    def columns: Ex[Int] = Columns(this)

    def columns_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyColumns, x)
    }
  }
}
trait GridPanel extends Panel {
  type C = scala.swing.GridPanel

  var rows    : Ex[Int]
  var columns : Ex[Int]
}
