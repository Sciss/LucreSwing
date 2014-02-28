/*
 *  StringExprEditor.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import scala.swing.TextField
import de.sciss.lucre.event.Sys
import impl.{StringExprEditorImpl => Impl}
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm
import de.sciss.desktop.UndoManager

object StringExprEditor {
  def apply[S <: Sys[S]](expr: Expr[S, String], name: String, columns: Int = 16)
                        (implicit tx: S#Tx, cursor: stm.Cursor[S], undoManager: UndoManager): StringExprEditor[S] =
    Impl(expr, name = name, columns = columns)
}
trait StringExprEditor[S <: Sys[S]] extends View[S] {
  override def component: TextField
}