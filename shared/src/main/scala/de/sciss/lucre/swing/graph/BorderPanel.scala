/*
 *  BorderPanel.scala
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

package de.sciss.lucre.swing
package graph

import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IControl}
import de.sciss.lucre.swing.graph.impl.PanelImpl
import de.sciss.lucre.{IExpr, Txn}
import de.sciss.lucre.swing.graph.impl.BorderPanelExpandedImpl

object BorderPanel extends ProductReader[BorderPanel] {
  def apply(north : Widget = Empty(),
            south : Widget = Empty(),
            west  : Widget = Empty(),
            east  : Widget = Empty(),
            center: Widget = Empty()
           ): BorderPanel =
    Impl(north = north, south = south, west = west, east = east, center = center)

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): BorderPanel = {
    require (arity == 5 && adj == 0)
    val _north  = in.readProductT[Widget]()
    val _south  = in.readProductT[Widget]()
    val _west   = in.readProductT[Widget]()
    val _east   = in.readProductT[Widget]()
    val _center = in.readProductT[Widget]()
    BorderPanel(_north, _south, _west, _east, _center)
  }

  object HGap extends ProductReader[HGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): HGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[BorderPanel]()
      new HGap(_w)
    }
  }
  final case class HGap(w: BorderPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"BorderPanel$$HGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyHGap)
      valueOpt.getOrElse(Const(defaultHGap)).expand[T]
    }
  }

  object VGap extends ProductReader[VGap] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): VGap = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[BorderPanel]()
      new VGap(_w)
    }
  }
  final case class VGap(w: BorderPanel) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"BorderPanel$$VGap" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyVGap)
      valueOpt.getOrElse(Const(defaultVGap)).expand[T]
    }
  }

  private final case class Impl(north: Widget, south: Widget, west: Widget, east: Widget,
                                center: Widget) extends BorderPanel with PanelImpl {
    override def productPrefix = "BorderPanel" // s"BorderPanel$$Impl" // serialization

    def contents: Seq[Widget] = {
      var res = List.empty[Widget]
      if (!center.isInstanceOf[Empty]) res ::= center
      if (!east  .isInstanceOf[Empty]) res ::= east
      if (!west  .isInstanceOf[Empty]) res ::= west
      if (!south .isInstanceOf[Empty]) res ::= south
      if (!north .isInstanceOf[Empty]) res ::= north
      res
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

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new BorderPanelExpandedImpl[T](this).initComponent()
  }

  private[graph] final val keyHGap     = "hGap"
  private[graph] final val keyVGap     = "vGap"
  private[graph] final val defaultHGap = 4
  private[graph] final val defaultVGap = 2
}
trait BorderPanel extends Panel {
  type C = View.Component // scala.swing.BorderPanel

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  def north : Widget
  def south : Widget
  def west  : Widget
  def east  : Widget
  def center: Widget

  /** Horizontal gap between components. The default value is 4. */
  var hGap: Ex[Int]

  /** Vertical gap between components. The default value is 2. */
  var vGap: Ex[Int]
}
