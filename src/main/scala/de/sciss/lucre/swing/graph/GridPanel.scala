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

import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.swingplus.{GridPanel => Peer}

object GridPanel {
  def apply(contents: Widget*): GridPanel = Impl(contents)

  private final val keyRows               = "rows"
  private final val keyColumns            = "columns"
  private final val keyCompact            = "compact"
  private final val keyCompactRows        = "compactRows"
  private final val keyCompactColumns     = "compactColumns"

  private final val defaultRows           = 0 // 1
  private final val defaultColumns        = 0
  private final val defaultCompact        = false
  private final val defaultCompactRows    = false
  private final val defaultCompactColumns = false

  private final class Expanded[S <: Sys[S]](protected val w: GridPanel) extends View[S]
    with ComponentHolder[Peer] with ComponentExpandedImpl[S] {

    type C = Peer

    override def init()(implicit tx: S#Tx, ctx: Ex.Context[S]): this.type = {
      val rows0           = ctx.getProperty[Ex[Int    ]](w, keyRows    ).fold(defaultRows    )(_.expand[S].value)
      val columns         = ctx.getProperty[Ex[Int    ]](w, keyColumns ).fold(defaultColumns )(_.expand[S].value)
      val compact         = ctx.getProperty[Ex[Boolean]](w, keyCompact ).exists(_.expand[S].value)
      val compactRows     = compact || ctx.getProperty[Ex[Boolean]](w, keyCompactRows   ).exists(_.expand[S].value)
      val compactColumns  = compact || ctx.getProperty[Ex[Boolean]](w, keyCompactColumns).exists(_.expand[S].value)
      val rows            = if (rows0 == 0 && columns == 0) 1 else 0  // not allowed to have both zero
      val contents        = w.contents.map(_.expand[S])
      deferTx {
        val vec           = contents.map(_.component)
        val p             = new Peer(rows0 = rows, cols0 = columns)
        p.compactRows     = compactRows
        p.compactColumns  = compactColumns
        p.contents      ++= vec
        component         = p
      }
      super.init()
    }
  }

  final case class Rows(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Rows" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyRows)
      valueOpt.getOrElse(Constant(defaultRows)).expand[S]
    }
  }

  final case class Columns(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Columns" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Constant(defaultColumns)).expand[S]
    }
  }

  final case class Compact(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$Compact" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompact)
      valueOpt.getOrElse(Constant(defaultCompact)).expand[S]
    }
  }

  final case class CompactRows(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$CompactRows" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactRows)
      valueOpt.getOrElse(Constant(defaultCompactRows)).expand[S]
    }
  }

  final case class CompactColumns(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$CompactColumns" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactColumns)
      valueOpt.getOrElse(Constant(defaultCompactColumns)).expand[S]
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends GridPanel with ComponentImpl {
    override def productPrefix = "GridPanel" // s"GridPanel$$Impl" // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).init()

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

    def compact: Ex[Boolean] = Compact(this)

    def compact_=(x: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyCompact, x)
    }

    def compactRows: Ex[Boolean] = CompactRows(this)

    def compactRows_=(x: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyCompactRows, x)
    }

    def compactColumns: Ex[Boolean] = CompactColumns(this)

    def compactColumns_=(x: Ex[Boolean]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyCompactColumns, x)
    }
  }
}
trait GridPanel extends Panel {
  type C = Peer

  var rows    : Ex[Int]
  var columns : Ex[Int]

  var compact         : Ex[Boolean]
  var compactRows     : Ex[Boolean]
  var compactColumns  : Ex[Boolean]
}
