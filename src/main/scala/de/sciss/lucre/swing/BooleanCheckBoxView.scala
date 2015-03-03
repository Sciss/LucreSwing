/*
 *  BooleanCheckBoxView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.swing.impl.{BooleanCheckBoxViewImpl => Impl}
import de.sciss.lucre.{expr, stm}
import de.sciss.model.Change
import de.sciss.serial.Serializer

import scala.swing.CheckBox

object BooleanCheckBoxView {
  /** Creates a new view from an expression. The check box's label will initially be set to `name`. */
  def apply[S <: Sys[S]](expr: Expr[S, Boolean], name: String)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] = {
    implicit val booleanEx = de.sciss.lucre.expr.Boolean
    Impl.apply(CellView.expr(expr), name = name)
  }

  def cell[S <: Sys[S]](cell: CellView[S#Tx, Boolean], name: String)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] =
    Impl.apply(cell, name = name)

  /** Creates a new view from a map entry. The check box's label will initially be set to `name`. */
  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Boolean], Change[Boolean]], key: A, default: Boolean,
                              name: String)
                             (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                              cursor: stm.Cursor[S], undoManager: UndoManager): BooleanCheckBoxView[S] =
    Impl.fromMap(map, key = key, default = default, name = name)
}
trait BooleanCheckBoxView[S <: Sys[S]] extends View[S] {
  override def component: CheckBox
}