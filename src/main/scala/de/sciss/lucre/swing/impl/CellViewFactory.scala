/*
 *  ExprViewFactory.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2016 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.lucre.{event => evt}
import de.sciss.lucre.expr.{Expr, Type}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.edit.{EditCellView, EditExprMap}
import de.sciss.model.Change

import scala.concurrent.stm.Ref
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

  protected def mkMapCommitter[S <: Sys[S], K, Ex[~ <: Sys[~]] <: Expr[~, A]](map: evt.Map[S, K, Ex], key: K,
                                                                           default: A, name: String)
                                            (implicit tx: S#Tx, cursor: stm.Cursor[S],
                                             keyType: evt.Map.Key[K],
                                             tpe: Type.Expr[A, Ex]): (A, Option[Committer[S, A]]) = {
    import tpe.newConst
    val com = map.modifiableOption.map { mapMod =>
      val mapH = tx.newHandle(mapMod)
      new Committer[S, A] {
        def commit(newValue: A)(implicit tx: S#Tx): UndoableEdit = {
          EditExprMap[S, K, A, Ex](s"Change $name", map = mapH(), key = key, value = Some(newConst[S](newValue)))
        }
      }
    }
    val value0 = map.get(key).map(_.value).getOrElse(default)
    (value0, com)
  }

  protected def mkMapObserver[S <: Sys[S], K, Ex[~ <: Sys[~]] <: Expr[~, A]](map: evt.Map[S, K, Ex], key: K,
                                              view: CellViewFactory.View[A])
                                           (implicit tx: S#Tx): Disposable[S#Tx] =
    new MapObserver(map, key, view, tx)

  private[this] final class MapObserver[S <: Sys[S], K, Ex[~ <: Sys[~]] <: Expr[~, A]](map: evt.Map[S, K, Ex], key: K,
                                                                                       view: CellViewFactory.View[A], tx0: S#Tx)
    extends Disposable[S#Tx] {

    private val valObs = Ref(null: Disposable[S#Tx])
    private val mapObs = map.changed.react {
      implicit tx => upd => upd.changes.foreach {
        case evt.Map.Added  (`key`, expr) =>
          valueAdded(expr)
          // XXX TODO -- if we moved this into `valueAdded`, the contract
          // could be that initially the view is updated
          val now0 = expr.value
          deferTx(view.update(now0))
        case evt.Map.Removed(`key`, _   ) =>
          valueRemoved()
        case _ =>
      }
    } (tx0)

    map.get(key)(tx0).foreach(valueAdded(_)(tx0))

    private def valueAdded(expr: Ex[S])(implicit tx: S#Tx): Unit = {
      val res = expr.changed.react { implicit tx => {
        case Change(_, now) =>
          deferTx(view.update(now))
        case _ =>  // XXX TODO -- should we ask for expr.value ?
      }}
      val v = valObs.swap(res)(tx.peer)
      if (v != null) v.dispose()
    }

    private def valueRemoved()(implicit tx: S#Tx): Boolean = {
      val v = valObs.swap(null)(tx.peer)
      val res = v != null
      if (res) v.dispose()
      res
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      valueRemoved()
      mapObs.dispose()
    }
  }
}