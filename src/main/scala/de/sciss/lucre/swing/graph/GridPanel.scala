/*
 *  GridPanel.scala
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

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{PanelExpandedImpl, PanelImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.swingplus.{GridPanel => Peer}

object GridPanel {
  def apply(contents: Widget*): GridPanel = Impl(contents)

  private final val keyRows               = "rows"
  private final val keyColumns            = "columns"
  private final val keyCompact            = "compact"
  private final val keyCompactRows        = "compactRows"
  private final val keyCompactColumns     = "compactColumns"
  private final val keyHGap               = "hGap"
  private final val keyVGap               = "vGap"

  private final val defaultRows           = 0 // 1
  private final val defaultColumns        = 0
  private final val defaultCompact        = false
  private final val defaultCompactRows    = false
  private final val defaultCompactColumns = false
  private final val defaultHGap           = 4
  private final val defaultVGap           = 2

  private final class Expanded[S <: Sys[S]](protected val peer: GridPanel) extends View[S]
    with ComponentHolder[Peer] with PanelExpandedImpl[S] {

    type C = Peer

    override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
      val rows0           = ctx.getProperty[Ex[Int    ]](peer, keyRows    ).fold(defaultRows    )(_.expand[S].value)
      val columns         = ctx.getProperty[Ex[Int    ]](peer, keyColumns ).fold(defaultColumns )(_.expand[S].value)
      val compact         = ctx.getProperty[Ex[Boolean]](peer, keyCompact ).exists(_.expand[S].value)
      val hGap            = ctx.getProperty[Ex[Int    ]](peer, keyHGap    ).fold(defaultHGap    )(_.expand[S].value)
      val vGap            = ctx.getProperty[Ex[Int    ]](peer, keyVGap    ).fold(defaultVGap    )(_.expand[S].value)
      val compactRows     = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactRows   ).exists(_.expand[S].value)
      val compactColumns  = compact || ctx.getProperty[Ex[Boolean]](peer, keyCompactColumns).exists(_.expand[S].value)
      val rows            = if (rows0 == 0 && columns == 0) 1 else 0  // not allowed to have both zero
      val contents        = peer.contents.map(_.expand[S])
      deferTx {
        val vec           = contents.map(_.component)
        val p             = new Peer(rows0 = rows, cols0 = columns)
        p.compactRows     = compactRows
        p.compactColumns  = compactColumns
        p.hGap            = hGap
        p.vGap            = vGap
        p.contents      ++= vec
        component         = p
      }
      super.initComponent()
    }
  }

  final case class Rows(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Rows" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyRows)
      valueOpt.getOrElse(Const(defaultRows)).expand[S]
    }
  }

  final case class Columns(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$Columns" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Const(defaultColumns)).expand[S]
    }
  }

  final case class Compact(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$Compact" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompact)
      valueOpt.getOrElse(Const(defaultCompact)).expand[S]
    }
  }

  final case class CompactRows(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$CompactRows" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactRows)
      valueOpt.getOrElse(Const(defaultCompactRows)).expand[S]
    }
  }

  final case class CompactColumns(w: GridPanel) extends Ex[Boolean] {
    override def productPrefix: String = s"GridPanel$$CompactColumns" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Boolean] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactColumns)
      valueOpt.getOrElse(Const(defaultCompactColumns)).expand[S]
    }
  }

  final case class HGap(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$HGap" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[S]
    }
  }

  final case class VGap(w: GridPanel) extends Ex[Int] {
    override def productPrefix: String = s"GridPanel$$VGap" // serialization

    def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): IExpr[S, Int] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[S]
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends GridPanel with PanelImpl {
    override def productPrefix = "GridPanel" // s"GridPanel$$Impl" // serialization

    protected def mkControl[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new Expanded[S](this).initComponent()

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

    def hGap: Ex[Int] = HGap(this)

    def hGap_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyHGap, x)
    }

    def vGap: Ex[Int] = VGap(this)

    def vGap_=(x: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyVGap, x)
    }
  }
}

/** A panel that arranges its contents in rectangular grid of rows and columns.
  * Note that components run from left to right, top to bottom.
  */
trait GridPanel extends Panel {
  type C = Peer

  type Repr[S <: Sys[S]] = View.T[S, C] with IControl[S]

  /** Number of rows or zero for automatic determination. */
  var rows    : Ex[Int]
  /** Number of columns or zero for automatic determination. */
  var columns : Ex[Int]

  /** `true` to make the grid compact both horizontally and vertically. */
  var compact         : Ex[Boolean]

  /** `true` to make the grid vertically compact. */
  var compactRows     : Ex[Boolean]

  /** `true` to make the grid horizontally compact. */
  var compactColumns  : Ex[Boolean]

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components. The default value is 2. */
  var vGap: Ex[Int]
}
