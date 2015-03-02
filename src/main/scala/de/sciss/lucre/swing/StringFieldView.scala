/*
 *  StringFieldView.scala
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
import de.sciss.lucre.swing.impl.{StringFieldViewImpl => Impl}
import de.sciss.lucre.{expr, stm}
import de.sciss.model.Change
import de.sciss.serial.Serializer

import scala.swing.TextField

object StringFieldView {
  def apply[S <: Sys[S]](expr: Expr[S, String], name: String, columns: Int = 16)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] =
    Impl.fromExpr(expr, name = name, columns = columns)

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, String], Change[String]], key: A, default: String,
                              name: String, columns: Int = 16)
                             (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                              cursor: stm.Cursor[S], undoManager: UndoManager): StringFieldView[S] =
    Impl.fromMap(map, key = key, default = default, name = name, columns = columns)
}
trait StringFieldView[S <: Sys[S]] extends View[S] {
  override def component: TextField
}