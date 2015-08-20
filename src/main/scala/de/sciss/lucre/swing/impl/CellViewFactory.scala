/*
 *  ExprViewFactory.scala
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
package impl

import javax.swing.undo.UndoableEdit

import de.sciss.lucre.expr.{Expr, Type}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.edit.{EditCellView, EditExprMap}
import de.sciss.lucre.{expr, stm}

import scala.language.{existentials, higherKinds}

object CellViewFactory {
  trait View[A] {
    def update(value: A): Unit
  }

  trait Committer[S <: Sys[S], A] {
    def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit
  }

  def mkCommitter[S <: Sys[S], A](cell: CellView[S#Tx, A], name: String)
                                 (implicit tx: S#Tx, cursor: stm.Cursor[S]): (A, Option[Committer[S, A]]) = {
    val com = CellView.Var.unapply(cell).map { vr =>
      new Committer[S, A] {
        def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit =
          EditCellView[S, A](s"Change $name", cell = vr, value = newValue)
      }
    }
    val value0 = cell()
    (value0, com)
  }

  def mkObserver[S <: Sys[S], A](cell: CellView[S#Tx, A], view: CellViewFactory.View[A])
                                (implicit tx: S#Tx): Disposable[S#Tx] =
    cell.react {
      implicit tx => upd => deferTx(view.update(upd))
    }
}
trait CellViewFactory[A] {
  import CellViewFactory.Committer

  protected def mkMapCommitter[S <: Sys[S], K](map: expr.Map[S, K, Expr[S, A]], key: K, default: A,
                                               name: String)
                                            (implicit tx: S#Tx, cursor: stm.Cursor[S],
                                             keyType: Type.Expr[K],
                                             tpe: Type.Expr[A]): (A, Option[Committer[S, A]]) = {
    import tpe.newConst
    val com = map.modifiableOption.map { mapMod =>
      val mapH = tx.newHandle(mapMod)
      new Committer[S, A] {
        def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit = {
          EditExprMap[S, K, A](s"Change $name", map = mapH(), key = key, value = Some(newConst[S](newValue)))
        }
      }
    }
    val value0 = map.get(key).map(_.value).getOrElse(default)
    (value0, com)
  }

  protected def mkMapObserver[S <: Sys[S], K](map: expr.Map[S, K, Expr[S, A]], key: K,
                                              view: CellViewFactory.View[A])
                                           (implicit tx: S#Tx): Disposable[S#Tx] =
    map.changed.react {
      implicit tx => upd => upd.changes.foreach {
        case expr.Map.Added  (`key`, expr) =>
          val now = expr.value
          deferTx(view.update(now))
// ELEM
//        case expr.Map.Element(`key`, expr, Change(_, now)) =>
//          deferTx(view.update(now))
        case _ =>
      }
    }
}
