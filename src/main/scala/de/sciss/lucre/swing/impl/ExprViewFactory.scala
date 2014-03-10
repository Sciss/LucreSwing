/*
 *  ExprViewFactory.scala
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
package impl

import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.{Expr, ExprType}
import de.sciss.lucre.{expr, stm}
import javax.swing.undo.UndoableEdit
import de.sciss.lucre.swing.edit.{EditExprMap, EditExprVar}
import de.sciss.model.Change
import de.sciss.serial.Serializer
import language.{existentials, higherKinds}
import de.sciss.lucre.stm.Disposable

object ExprViewFactory {
  trait View[A] {
    def update(value: A): Unit
  }

  trait Committer[S <: Sys[S], A] {
    def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit
  }
}
trait ExprViewFactory[A] {
  import ExprViewFactory.Committer

  protected def mkExprCommitter[S <: Sys[S]](expr: Expr[S, A], name: String)
                                            (implicit tx: S#Tx, cursor: stm.Cursor[S],
                                             tpe: ExprType[A]): (A, Option[Committer[S, A]]) = {
    import tpe.{serializer, varSerializer, newConst}
    val com = Expr.Var.unapply(expr).map { vr =>
      val exprVarH = tx.newHandle(vr)
      new Committer[S, A] {
        def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit = {
          EditExprVar[S, A](s"Change $name", expr = exprVarH(), value = newConst[S](newValue))
        }
      }
    }
    val value0 = expr.value
    (value0, com)
  }

  protected def mkExprObserver[S <: Sys[S]](expr: Expr[S, A], view: ExprViewFactory.View[A])
                                           (implicit tx: S#Tx): Disposable[S#Tx] =
    expr.changed.react {
      implicit tx => upd => view.update(upd.now)
    }

  protected def mkMapCommitter[S <: Sys[S], K](map: expr.Map[S, K, Expr[S, A], Change[A]], key: K, default: A,
                                               name: String)
                                            (implicit tx: S#Tx, cursor: stm.Cursor[S],
                                             keySerializer: Serializer[S#Tx, S#Acc, K],
                                             tpe: ExprType[A]): (A, Option[Committer[S, A]]) = {
    import tpe.{serializer, newConst}
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

  protected def mkMapObserver[S <: Sys[S], K](map: expr.Map[S, K, Expr[S, A], Change[A]], key: K,
                                              view: ExprViewFactory.View[A])
                                           (implicit tx: S#Tx): Disposable[S#Tx] =
    map.changed.react {
      implicit tx => upd => upd.changes.foreach {
        case expr.Map.Added  (`key`, expr)                  => view.update(expr.value)
        // case expr.Map.Removed(`key`, expr)                  => res.update(ha?)
        case expr.Map.Element(`key`, expr, Change(_, now))  => view.update(now       )
        case _ =>
      }
    }
}
