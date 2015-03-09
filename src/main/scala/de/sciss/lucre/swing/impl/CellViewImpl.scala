/*
 *  CellViewImpl.scala
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

package de.sciss.lucre
package swing
package impl

import de.sciss.lucre.event.Sys
import de.sciss.lucre.expr.ExprType
import de.sciss.lucre.stm.Disposable
import de.sciss.model.Change
import de.sciss.serial.Serializer

object CellViewImpl {
  trait Basic[Tx, A] extends CellView[Tx, A] {
    def map[B](f: A => B): CellView[Tx, B] = new MapImpl(this, f)
  }

  private[swing] trait ExprMapLike[S <: Sys[S], K, A, Ex <: expr.Expr[S, A], U]
    extends Basic[S#Tx, Option[A]] {

    protected def h: stm.Source[S#Tx, expr.Map[S, K, Ex, U]]
    protected val key: K
    protected def mapUpdate(u: U): Option[A]

    type Repr = Option[Ex]

    def react(fun: S#Tx => Option[A] => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      h().changed.react { implicit tx => u =>
        u.changes.foreach {
          case expr.Map.Added  (`key`, ex   ) => fun(tx)(Some(ex.value))
          case expr.Map.Removed(`key`, _    ) => fun(tx)(None)
          case expr.Map.Element(`key`, _, ch) =>
            val opt = mapUpdate(ch)
            if (opt.isDefined) fun(tx)(opt)
          case _ =>
        }
      }

    def repr(implicit tx: S#Tx): Repr = h().get(key)

    def apply()(implicit tx: S#Tx): Option[A] = repr.map(_.value)
  }

  private[swing] final class ExprMap[S <: Sys[S], K, A, Ex <: expr.Expr[S, A], U](protected val h: stm.Source[S#Tx, expr.Map[S, K, Ex, U]],
                                                                                  protected val key: K,
                                                                                  val updFun: U => Option[A])
    extends ExprMapLike[S, K, A, Ex, U] {

    protected def mapUpdate(u: U): Option[A] = updFun(u)
  }

  private[swing] final class ExprModMap[S <: Sys[S], K, A](protected val h: stm.Source[S#Tx, expr.Map.Modifiable[S, K, expr.Expr[S, A], Change[A]]],
                                                           protected val key: K)
                                                          (implicit tpe: ExprType[A])
    extends ExprMapLike[S, K, A, expr.Expr[S, A], Change[A]] with CellView.Var[S, Option[A]] {

    def serializer: Serializer[S#Tx, S#Acc, Repr] = {
      implicit val exSer = tpe.serializer[S]
      Serializer.option[S#Tx, S#Acc, expr.Expr[S, A]]
    }

    protected def mapUpdate(ch: Change[A]): Option[A] = if (ch.isSignificant) Some(ch.now) else None

    def repr_=(value: Repr)(implicit tx: S#Tx): Unit = value.fold[Unit] {
      h().remove(key)
    } { ex =>
      val map = h()
      map.get(key) match {
        case Some(expr.Expr.Var(vr)) => vr() = ex
        case _ =>
          val exV = expr.Expr.Var.unapply(ex).getOrElse(tpe.newVar(ex))
          map.put(key, exV)
      }
    }

    def lift(value: Option[A])(implicit tx: S#Tx): Repr = value.map(tpe.newConst)

    def update(v: Option[A])(implicit tx: S#Tx): Unit = repr_=(lift(v))
  }

  private[swing] trait ExprLike[S <: Sys[S], A, Ex <: expr.Expr[S, A]] extends Basic[S#Tx, A] {
    type Repr = Ex

    protected def h: stm.Source[S#Tx, Ex]

    def react(fun: S#Tx => A => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      h().changed.react { implicit tx => ch => fun(tx)(ch.now) }

    def apply()(implicit tx: S#Tx): A = h().value

    def repr(implicit tx: S#Tx): Repr = h()
  }

  private[swing] final class Expr[S <: Sys[S], A, Ex <: expr.Expr[S, A]](protected val h: stm.Source[S#Tx, Ex])
    extends ExprLike[S, A, Ex]

  private[swing] final class ExprVar[S <: Sys[S], A](protected val h: stm.Source[S#Tx, expr.Expr.Var[S, A]])
                                                    (implicit tpe: ExprType[A])
    extends ExprLike[S, A, expr.Expr[S, A]] with CellView.Var[S, A] {

    def repr_=(value: expr.Expr[S, A])(implicit tx: S#Tx): Unit = h().update(value)

    def lift(value: A)(implicit tx: S#Tx): Repr = tpe.newConst(value)

    def update(v: A)(implicit tx: S#Tx): Unit = repr_=(lift(v))

    def serializer: Serializer[S#Tx, S#Acc, Repr] = tpe.serializer
  }

  private[swing] sealed trait NoVar[Tx, A] extends Basic[Tx, A] {
    type Repr = Unit

    final def repr(implicit tx: Tx) = ()
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

  //  private[swing] final class Var[S <: Sys[S], A](init: A)
  //    extends NoVar[S#Tx, A] with CellView.Var[S, A] with ObservableImpl[S, A] {
  //
  //    private val ref = Ref(init)
  //
  //    def apply()(implicit tx: S#Tx): A = ref.get(tx.peer)
  //
  //    def update(v: A)(implicit tx: S#Tx): Unit = {
  //      val old = ref.swap(v)(tx.peer)
  //      if (v != old) fire(v)
  //    }
  //
  //    def lift(value: A)(implicit tx: S#Tx) = ()
  //
  //    def repr_=(value: Repr)(implicit tx: S#Tx) = ()
  //
  //    def serializer: Serializer[S#Tx, S#Acc, Repr] = ImmutableSerializer.Unit
  //  }
}
