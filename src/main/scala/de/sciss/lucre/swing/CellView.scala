/*
 *  CellView.scala
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

package de.sciss.lucre.swing

import de.sciss.lucre.event.Observable
import de.sciss.lucre.expr.{Expr, Type}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.impl.{CellViewImpl => Impl}
import de.sciss.lucre.{event => evt, expr => _expr, stm}
import de.sciss.serial.Serializer

import scala.language.higherKinds

object CellView {
  def expr[S <: Sys[S], A, Ex[~ <: Sys[~]] <: Expr[~, A]](x: Ex[S])(implicit tx: S#Tx,
                                          tpe: Type.Expr[A, Ex]): CellView[S#Tx, A] { type Repr = Ex[S] } = {
    import tpe.{serializer, varSerializer}
    x match {
      case tpe.Var(vr) =>
        new Impl.ExprVar[S, A, Ex](tx.newHandle(vr))

      case _ =>
        new Impl.Expr[S, A, Ex](tx.newHandle(x))
    }
  }

  def exprMap[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: _expr.Expr[~, A]](map: evt.Map[S, K, Ex], key: K)
                                (implicit tx: S#Tx, tpe: Type.Expr[A, Ex], keyType: evt.Map.Key[K])
  : CellView[S#Tx, Option[A]] { type Repr = Option[Ex[S]] } = {
    // import tpe.serializer
    map.modifiableOption.fold[CellView[S#Tx, Option[A]] { type Repr = Option[Ex[S]] }] {
      new Impl.ExprMap[S, K, A, Ex  /* , Change[A] */](tx.newHandle(map), key /* , ch =>
        if (ch.isSignificant) Some(ch.now) else None */)
    } { mv =>
      new Impl.ExprModMap[S, K, A, Ex](tx.newHandle(mv), key)
    }
  }

  def exprLike[S <: Sys[S], A, Ex[~ <: Sys[~]] <: Expr[~, A]](x: Ex[S])
                                                (implicit tx: S#Tx,
                                                 serializer: Serializer[S#Tx, S#Acc, Ex[S]]): CellView[S#Tx, A] { type Repr = Ex[S] } =
    new Impl.Expr[S, A, Ex](tx.newHandle(x))

  def const[S <: Sys[S], A](value: A): CellView[S#Tx, A] = new Impl.Const(value)

  object Var {
    def unapply[S <: Sys[S], A](view: CellView[S#Tx, A]): Option[Var[S, A]] = view match {
      case vr: Var[S, A] => Some(vr)
      case _ => None
    }
  }
  trait Var[S <: Sys[S], A] extends CellView[S#Tx, A] with stm.Sink[S#Tx, A] {
    def repr_=(value: Repr)(implicit tx: S#Tx): Unit

    def lift(value: A)(implicit tx: S#Tx): Repr

    implicit def serializer: Serializer[S#Tx, S#Acc, Repr]
  }

//  object CanUpdate {
//    def apply[S <: Sys[S], A](init: CellView[S#Tx, A])(implicit tx: S#Tx): CanUpdate[S, A] =
//      new Impl.CanUpdate[S, A](init, tx)
//  }
//  trait CanUpdate[S <: Sys[S], A] extends CellView[S#Tx, A] {
//    type Repr = CellView[S#Tx, A]
//
//    def repr_=(repr: Repr)(implicit tx: S#Tx): Unit
//  }
}
trait CellView[Tx, +A] extends Observable[Tx, A] with stm.Source[Tx, A] {
  def map[B](f: A => B): CellView[Tx, B]

  type Repr

  def repr(implicit tx: Tx): Repr
}