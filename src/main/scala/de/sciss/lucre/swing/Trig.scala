package de.sciss.lucre.swing

import de.sciss.lucre.aux.ProductWithAux
import de.sciss.lucre.event.IPublisher
import de.sciss.lucre.expr.Ex
import de.sciss.lucre.expr.Ex.Context
import de.sciss.lucre.stm.{Base, Disposable, Sys}

object Trig {
  trait IExpr[S <: Base[S]] extends IPublisher[S, Unit] with Disposable[S#Tx]
}
trait Trig extends ProductWithAux {
  def | (t: Trig): Trig
  def & (t: Trig): Trig
  def ^ (t: Trig): Trig

  def filter    (ex: Ex[Boolean]): Trig
  def filterNot (ex: Ex[Boolean]): Trig

  def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Trig.IExpr[S]
}
