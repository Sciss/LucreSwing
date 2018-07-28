/*
 *  ExAttrUpdate.scala
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

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.expr.{CellView, Ex, IExpr, Type}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing

object ExAttrUpdate {
  private final class Expanded[S <: Sys[S], A](source: IExpr[S, A], attrView: CellView.Var[S, Option[A]], tx0: S#Tx)
    extends Disposable[S#Tx] {

    private[this] val obs = source.changed.react { implicit tx => upd =>
      val value = Some(upd.now)
      attrView.update(value)
    } (tx0)

    def dispose()(implicit tx: S#Tx): Unit =
      obs.dispose()
  }
}
final case class ExAttrUpdate[A](source: Ex[A], sink: String)(implicit tpe: Type.Aux[A])
  extends Control {

  protected def mkControl[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): Disposable[S#Tx] =
    b.selfOption.fold(Disposable.empty[S#Tx]) { self =>
      val attrView = CellView.attr[S, A, tpe.E](self.attr, sink)(tx, tpe.peer)
      new swing.ExAttrUpdate.Expanded[S, A](source.expand[S], attrView, tx)
    }

  def aux: scala.List[Aux] = tpe :: Nil
}
