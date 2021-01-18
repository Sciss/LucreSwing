/*
 *  GridPanel.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl}
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.GridPanelExpandedImpl
import de.sciss.lucre.swing.graph.impl.PanelImpl
import de.sciss.lucre.{IExpr, Txn}

object GridPanel extends ProductReader[GridPanel] {
  def apply(contents: Widget*): GridPanel = Impl(contents)

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): GridPanel = {
    require (arity == 1 && adj == 0)
    val _contents = in.readVec(in.readProductT[Widget]())
    GridPanel(_contents: _*)
  }

  private[graph] final val keyRows               = "rows"
  private[graph] final val keyColumns            = "columns"
  private[graph] final val keyCompact            = "compact"
  private[graph] final val keyCompactRows        = "compactRows"
  private[graph] final val keyCompactColumns     = "compactColumns"
  private[graph] final val keyHGap               = "hGap"
  private[graph] final val keyVGap               = "vGap"

  private[graph] final val defaultRows           = 0 // 1
  private[graph] final val defaultColumns        = 0
  private[graph] final val defaultCompact        = false
  private[graph] final val defaultCompactRows    = false
  private[graph] final val defaultCompactColumns = false
  private[graph] final val defaultHGap           = 4
  private[graph] final val defaultVGap           = 2

  object Rows extends ProductReader[Rows] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Rows = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new Rows(_w)
    }
  }
  final case class Rows(w: GridPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"GridPanel$$Rows" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyRows)
      valueOpt.getOrElse(Const(defaultRows)).expand[T]
    }
  }

  object Columns extends ProductReader[Columns] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Columns = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new Columns(_w)
    }
  }
  final case class Columns(w: GridPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"GridPanel$$Columns" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyColumns)
      valueOpt.getOrElse(Const(defaultColumns)).expand[T]
    }
  }

  object Compact extends ProductReader[Compact] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Compact = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new Compact(_w)
    }
  }
  final case class Compact(w: GridPanel) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"GridPanel$$Compact" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompact)
      valueOpt.getOrElse(Const(defaultCompact)).expand[T]
    }
  }

  object CompactRows extends ProductReader[CompactRows] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): CompactRows = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new CompactRows(_w)
    }
  }
  final case class CompactRows(w: GridPanel) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"GridPanel$$CompactRows" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactRows)
      valueOpt.getOrElse(Const(defaultCompactRows)).expand[T]
    }
  }

  object CompactColumns extends ProductReader[CompactColumns] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): CompactColumns = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new CompactColumns(_w)
    }
  }
  final case class CompactColumns(w: GridPanel) extends Ex[Boolean] {
    type Repr[T <: Txn[T]] = IExpr[T, Boolean]

    override def productPrefix: String = s"GridPanel$$CompactColumns" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Boolean]](w, keyCompactColumns)
      valueOpt.getOrElse(Const(defaultCompactColumns)).expand[T]
    }
  }

  object HGap extends ProductReader[HGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): HGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new HGap(_w)
    }
  }
  final case class HGap(w: GridPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"GridPanel$$HGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[T]
    }
  }

  object VGap extends ProductReader[VGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): VGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[GridPanel]()
      new VGap(_w)
    }
  }
  final case class VGap(w: GridPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"GridPanel$$VGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[T]
    }
  }

  private final case class Impl(contents: Seq[Widget]) extends GridPanel with PanelImpl {
    override def productPrefix = "GridPanel" // s"GridPanel$$Impl" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new GridPanelExpandedImpl[T](this).initComponent()

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
  type C = View.Component // Peer

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

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
