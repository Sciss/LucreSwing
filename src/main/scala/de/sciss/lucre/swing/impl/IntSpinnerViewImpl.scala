/*
 *  IntSpinnerViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre
package swing
package impl

import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.expr.Expr
import de.sciss.desktop.UndoManager
import de.sciss.lucre.{expr, stm}
import javax.swing.SpinnerNumberModel
import de.sciss.model.Change
import de.sciss.serial.Serializer

object IntSpinnerViewImpl extends ExprViewFactory[Int] {
  def fromExpr[S <: Sys[S]](_expr: Expr[S, Int], name: String, width: Int)
                           (implicit tx: S#Tx, cursor: stm.Cursor[S],
                            undoManager: UndoManager): IntSpinnerView[S] = {
    // implicit val tpe: ExprType[Int] = expr.Int
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = mkExprCommitter(_expr, name)(tx, cursor, expr.Int)
      protected val observer: Disposable[S#Tx]  = mkExprObserver (_expr, impl)
    }

    deferTx(res.guiInit())
    res
  }

  def fromMap[S <: Sys[S], A](map: expr.Map[S, A, Expr[S, Int], Change[Int]], key: A, default: Int,
                              name: String, width: Int)
                             (implicit tx: S#Tx, keySerializer: Serializer[S#Tx, S#Acc, A],
                              cursor: stm.Cursor[S], undoManager: UndoManager): IntSpinnerView[S] = {
    val res = new Impl[S](maxWidth = width) {
      impl =>
      protected var (value, committer)          = mkMapCommitter(map, key, default, name)(
        tx, cursor, keySerializer, expr.Int)
      protected val observer: Disposable[S#Tx]  = mkMapObserver (map, key, impl)
    }

    deferTx(res.guiInit())
    res
  }

  private abstract class Impl[S <: Sys[S]](maxWidth: Int)
                                          (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
    extends NumberSpinnerViewImpl[S, Int](maxWidth) with IntSpinnerView[S] {

    final protected val tpe = expr.Int

    protected def parseModelValue(v: Any): Option[Int] = v match {
      case i: Int => Some(i)
      case _      => None
    }

    protected lazy val model: SpinnerNumberModel = new SpinnerNumberModel(value, Int.MinValue, Int.MaxValue, 1)
  }
}