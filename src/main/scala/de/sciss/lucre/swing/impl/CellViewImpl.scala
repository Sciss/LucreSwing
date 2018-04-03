/*
 *  CellViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
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

import de.sciss.lucre.expr.Type
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.{event => evt}
import de.sciss.model.Change
import de.sciss.serial.Serializer

import scala.concurrent.stm.Ref
import scala.language.higherKinds

object CellViewImpl {
  trait Basic[Tx, A] extends CellView[Tx, A] {
    def map[B](f: A => B): CellView[Tx, B] = new MapImpl(this, f)
  }

  private[swing] trait ExprMapLike[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: expr.Expr[~, A] /* , U */]
    extends Basic[S#Tx, Option[A]] {

    protected def h: stm.Source[S#Tx, evt.Map[S, K, Ex]]
    protected val key: K
    // protected def mapUpdate(u: U): Option[A]

    type Repr = Option[Ex[S]]

    def react(fun: S#Tx => Option[A] => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      new ExprMapLikeObs(h(), key, fun, tx)

    // XXX TODO -- remove in next major version
    def repr(implicit tx: S#Tx): Repr = throw new Exception("Subclass responsibility")

    def apply()(implicit tx: S#Tx): Option[A] = repr.map(_.value)
  }

  private[this] final class ExprMapLikeObs[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: expr.Expr[~, A], U](
      map: evt.Map[S, K, Ex], key: K, fun: S#Tx => Option[A] => Unit, tx0: S#Tx)
    extends Disposable[S#Tx] {

    private val valObs = Ref(null: Disposable[S#Tx])

    private val mapObs = map.changed.react { implicit tx => u =>
      u.changes.foreach {
        case evt.Map.Added  (`key`, expr) =>
          valueAdded(expr)
          // XXX TODO -- if we moved this into `valueAdded`, the contract
          // could be that initially the view is updated
          val now0 = expr.value
          fun(tx)(Some(now0))
        case evt.Map.Removed(`key`, _ ) =>
          if (valueRemoved()) fun(tx)(None)
        case _ =>
      }
    } (tx0)

    map.get(key)(tx0).foreach(valueAdded(_)(tx0))

    private def valueAdded(expr: Ex[S])(implicit tx: S#Tx): Unit = {
      val res = expr.changed.react { implicit tx => {
        case Change(_, now) =>
          fun(tx)(Some(now))
        //            val opt = mapUpdate(ch)
        //            if (opt.isDefined) fun(tx)(opt)
        case _ =>  // XXX TODO -- should we ask for expr.value ?
      }}
      val v = valObs.swap(res)
      if (v != null) v.dispose()
    }

    private def valueRemoved()(implicit tx: S#Tx): Boolean = {
      val v   = valObs.swap(null)
      val res = v != null
      if (res) v.dispose()
      res
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      valueRemoved()
      mapObs.dispose()
    }
  }

//  private[swing] final class CanUpdate[S <: Sys[S], A](init: CellView[S#Tx, A], tx0: S#Tx)
//    extends CellView.CanUpdate[S, A] with Basic[S#Tx, A] /* with ObservableImpl[S, A] */ {
//
//    private[this] val reprRef = Ref(init)
//    private[this] val reprObs = Ref(mkObs(init)(tx0))
//
//    private def mkObs(r: Repr)(implicit tx: S#Tx): Disposable[S#Tx] =
//      r.react { implicit tx => upd =>
//        fire(upd)
//      }
//
//    def repr(implicit tx: S#Tx): Repr = reprRef()
//
//    def repr_=(value: Repr)(implicit tx: S#Tx): Unit = {
//      reprRef() = value
//      val oldObs = reprObs.swap(mkObs(value))
//      oldObs.dispose()
//    }
//
//    def apply()(implicit tx: S#Tx): A = reprRef().apply()
//  }

  private[swing] final class ExprMap[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: expr.Expr[~, A] /* , U */](
      protected val h: stm.Source[S#Tx, evt.Map[S, K, Ex]],
      protected val key: K /* , val updFun: U => Option[A] */)
    extends ExprMapLike[S, K, A, Ex /* , U */] {

    override def repr(implicit tx: S#Tx): Repr = h().get(key)

    // protected def mapUpdate(u: U): Option[A] = updFun(u)
  }

  private[swing] final class ExprModMap[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]](
      protected val h: stm.Source[S#Tx, evt.Map.Modifiable[S, K, Ex]],
      protected val key: K)
     (implicit tpe: Type.Expr[A, Ex])
    extends ExprMapLike[S, K, A, Ex /* , Change[A] */] with CellView.Var[S, Option[A]] {

    def serializer: Serializer[S#Tx, S#Acc, Repr] = {
      implicit val exSer = tpe.serializer[S]
      Serializer.option[S#Tx, S#Acc, Ex[S]]
    }

    // protected def mapUpdate(ch: Change[A]): Option[A] = if (ch.isSignificant) Some(ch.now) else None

    override def repr(implicit tx: S#Tx): Repr = {
      val opt = h().get(key)
      // ! important to unwrap, otherwise we get infinite recursion with `repr = repr` !
      opt.map {
        case tpe.Var(vr) =>
          vr()
        case other => other
      }
    }

    def repr_=(value: Repr)(implicit tx: S#Tx): Unit = value.fold[Unit] {
      h().remove(key)
    } { ex =>
      val map = h()
      map.get(key) match {
        case Some(tpe.Var(vr)) => vr() = ex
        case _ =>
          val exV = tpe.Var.unapply(ex).getOrElse(tpe.newVar(ex))
          map.put(key, exV)
      }
    }

    def lift(value: Option[A])(implicit tx: S#Tx): Repr = value.map(tpe.newConst[S](_))

    def update(v: Option[A])(implicit tx: S#Tx): Unit = repr_=(lift(v))
  }

  private[swing] trait ExprLike[S <: Sys[S], A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]]
    extends Basic[S#Tx, A] {

    type Repr = Ex[S]

    protected def h: stm.Source[S#Tx, Repr]

    def react(fun: S#Tx => A => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      h().changed.react { implicit tx => ch => fun(tx)(ch.now) }

    def apply()(implicit tx: S#Tx): A = h().value

    // XXX TODO -- remove in next major version
    def repr(implicit tx: S#Tx): Repr = throw new Exception("Subclass responsibility")
  }

  private[swing] final class Expr[S <: Sys[S], A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]](
      protected val h: stm.Source[S#Tx, Ex[S]])
    extends ExprLike[S, A, Ex] {

    override def repr(implicit tx: S#Tx): Repr = h()
  }

  private[swing] final class ExprVar[S <: Sys[S], A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]](
      protected val h: stm.Source[S#Tx, Ex[S] with stm.Var[S#Tx, Ex[S]]])
     (implicit tpe: Type.Expr[A, Ex])
    extends ExprLike[S, A, Ex] with CellView.Var[S, A] {

    // ! important to unwrap, otherwise we get infinite recursion with `repr = repr` !
    override def repr(implicit tx: S#Tx): Repr = h().apply()

    def repr_=(value: Repr)(implicit tx: S#Tx): Unit = h().update(value)

    def lift(value: A)(implicit tx: S#Tx): Repr = tpe.newConst(value)

    def update(v: A)(implicit tx: S#Tx): Unit = repr_=(lift(v))

    def serializer: Serializer[S#Tx, S#Acc, Repr] = tpe.serializer
  }

  private[swing] sealed trait NoVar[Tx, A] extends Basic[Tx, A] {
    type Repr = Unit

    final def repr(implicit tx: Tx): Unit = ()
  }

  private[swing] final class MapImpl[Tx, A, B](in: CellView[Tx, A], f: A => B)
    extends NoVar[Tx, B] {

    def react(fun: Tx => B => Unit)(implicit tx: Tx): Disposable[Tx] =
      in.react { implicit tx => a => fun(tx)(f(a)) }

    def apply()(implicit tx: Tx): B = f(in())
  }

  object DummyDisposable extends Disposable[Any] {
    def dispose()(implicit tx: Any): Unit = ()
  }

  private[swing] final class Const[Tx, A](value: A)
    extends NoVar[Tx, A] {

     def react(fun: Tx => A => Unit)(implicit tx: Tx): Disposable[Tx] = DummyDisposable

    def apply()(implicit tx: Tx): A = value
  }
}
