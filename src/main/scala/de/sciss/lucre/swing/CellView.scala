/*
 *  CellView.scala
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

import de.sciss.lucre.event.{Observable, Sys}
import de.sciss.lucre.expr.{ExprType, Expr}
import de.sciss.lucre.stm
import de.sciss.lucre.swing.impl.{CellViewImpl => Impl}
import de.sciss.serial.Serializer

import scala.language.higherKinds

object CellView {
  def expr[S <: Sys[S], A](x: Expr[S, A])(implicit tx: S#Tx,
                                          tpe: ExprType[A]): CellView[S#Tx, A] { type Repr = Expr[S, A] } = {
    import tpe.{serializer, varSerializer}
    x match {
      case Expr.Var(vr) =>
        new Impl.ExprVar[S, A](tx.newHandle(vr))

      case _ =>
        new Impl.Expr[S, A, Expr[S, A]](tx.newHandle(x))
    }
  }

  def exprLike[S <: Sys[S], A, Ex <: Expr[S, A]](x: Ex)
                                                (implicit tx: S#Tx,
                                                 serializer: Serializer[S#Tx, S#Acc, Ex]): CellView[S#Tx, A] { type Repr = Ex } =
    new Impl.Expr[S, A, Ex](tx.newHandle(x))

  def const[S <: Sys[S], A](value: A): CellView[S#Tx, A] = new Impl.Const(value)

  //  /** Creates a view with an in-memory variable. */
  //  def apply[S <: Sys[S], A](init: A)(implicit tx: S#Tx): Var[S, A] = new Impl.Var(init)

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
}
trait CellView[Tx, +A] extends Observable[Tx, A] with stm.Source[Tx, A] {
  def map[B](f: A => B): CellView[Tx, B]

  type Repr

  def repr(implicit tx: Tx): Repr
}