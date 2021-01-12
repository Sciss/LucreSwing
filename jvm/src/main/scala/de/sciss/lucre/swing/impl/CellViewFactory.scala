/*
 *  ExprViewFactory.scala
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

package de.sciss.lucre.swing.impl

import de.sciss.lucre.expr.CellView
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.edit.{EditCellView, EditExprMap}
import de.sciss.lucre.{Cursor, Disposable, Expr, MapObj, Txn}
import de.sciss.model.Change
import javax.swing.undo.UndoableEdit

import scala.concurrent.stm.Ref

object CellViewFactory {
  trait View[A] {
    def update(value: A): Unit
  }

  trait Committer[T <: Txn[T], A] {
    def commit(newValue: A)(implicit tx: T): UndoableEdit
  }

  def mkCommitter[T <: Txn[T], A](cell: CellView[T, A], name: String)
                                 (implicit tx: T, cursor: Cursor[T]): (A, Option[Committer[T, A]]) = {
    val com = CellView.VarR.unapply(cell).map { vr =>
      new Committer[T, A] {
        def commit(newValue: A)(implicit tx: T): UndoableEdit =
          EditCellView[T, A](s"Change $name", cell = vr, value = newValue)
      }
    }
    val value0 = cell()
    (value0, com)
  }

  def mkObserver[T <: Txn[T], A](cell: CellView[T, A], view: CellViewFactory.View[A])
                                (implicit tx: T): Disposable[T] =
    cell.react {
      implicit tx => upd => deferTx(view.update(upd))
    }
}
trait CellViewFactory[A] {
  import CellViewFactory.Committer

  protected def mkMapCommitter[T <: Txn[T], K, Ex[~ <: Txn[~]] <: Expr[~, A]](map: MapObj[T, K, Ex], key: K,
                                                                           default: A, name: String)
                                            (implicit tx: T, cursor: Cursor[T],
                                             keyType: MapObj.Key[K],
                                             tpe: Expr.Type[A, Ex]): (A, Option[Committer[T, A]]) = {
    import tpe.newConst
    val com = map.modifiableOption.map { mapMod =>
      val mapH = tx.newHandle(mapMod)
      new Committer[T, A] {
        def commit(newValue: A)(implicit tx: T): UndoableEdit = {
          EditExprMap[T, K, A, Ex](s"Change $name", map = mapH(), key = key, value = Some(newConst[T](newValue)))
        }
      }
    }
    val value0 = map.get(key).map(_.value).getOrElse(default)
    (value0, com)
  }

  protected def mkMapObserver[T <: Txn[T], K, Ex[~ <: Txn[~]] <: Expr[~, A]](map: MapObj[T, K, Ex], key: K,
                                              view: CellViewFactory.View[A])
                                           (implicit tx: T): Disposable[T] =
    new MapObserver(map, key, view, tx)

  private[this] final class MapObserver[T <: Txn[T], K, Ex[~ <: Txn[~]] <: Expr[~, A]](map: MapObj[T, K, Ex], key: K,
                                                                                       view: CellViewFactory.View[A], tx0: T)
    extends Disposable[T] {

    private val valObs = Ref(null: Disposable[T])
    private val mapObs = map.changed.react {
      implicit tx => upd => upd.changes.foreach {
        case MapObj.Added  (`key`, expr) =>
          valueAdded(expr)
          // XXX TODO -- if we moved this into `valueAdded`, the contract
          // could be that initially the view is updated
          val now0 = expr.value
          deferTx(view.update(now0))
        case MapObj.Removed(`key`, _   ) =>
          valueRemoved()
        case _ =>
      }
    } (tx0)

    map.get(key)(tx0).foreach(valueAdded(_)(tx0))

    private def valueAdded(expr: Ex[T])(implicit tx: T): Unit = {
      val res = expr.changed.react { implicit tx => {
        case Change(_, now) =>
          deferTx(view.update(now))
        case _ =>  // XXX TODO -- should we ask for expr.value ?
      }}
      val v = valObs.swap(res)(tx.peer)
      if (v != null) v.dispose()
    }

    private def valueRemoved()(implicit tx: T): Boolean = {
      val v = valObs.swap(null)(tx.peer)
      val res = v != null
      if (res) v.dispose()
      res
    }

    def dispose()(implicit tx: T): Unit = {
      valueRemoved()
      mapObs.dispose()
    }
  }
}