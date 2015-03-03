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
import de.sciss.serial.Serializer

object CellViewImpl {
  trait Basic[Tx, A] extends CellView[Tx, A] {
    def map[B](f: A => B): CellView[Tx, B] = new MapImpl(this, f)
  }

  private[swing] final class Expr[S <: Sys[S], A, Ex <: expr.Expr[S, A]](h: stm.Source[S#Tx, Ex])
    extends Basic[S#Tx, A] {

    type Repr = Ex

    def react(fun: S#Tx => A => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      h().changed.react { implicit tx => ch => fun(tx)(ch.now) }

    def apply()(implicit tx: S#Tx): A = h().value

    def repr(implicit tx: S#Tx): Repr = h()
  }

  private[swing] final class ExprVar[S <: Sys[S], A](h: stm.Source[S#Tx, expr.Expr.Var[S, A]])
                                                    (implicit tpe: ExprType[A])
    extends Basic[S#Tx, A] with CellView.Var[S, A] {

    type Repr = expr.Expr[S, A]

    // XXX TODO -- DRY with `Expr`
    def react(fun: S#Tx => A => Unit)(implicit tx: S#Tx): Disposable[S#Tx] =
      h().changed.react { implicit tx => ch => fun(tx)(ch.now) }

    def apply()(implicit tx: S#Tx): A = h().value

    def repr(implicit tx: S#Tx): Repr = h().apply()

    def repr_=(value: Repr)(implicit tx: S#Tx): Unit = h().update(value)

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
