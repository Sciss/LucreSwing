/*
 *  Observation.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.Log.{swing => log}
import de.sciss.lucre.{Disposable, Publisher, Source, Txn}
import de.sciss.serial.TFormat

object Observation {
  def apply[T <: Txn[T], U, A](value: A with Publisher[T, U])(
      observe: T => U => Unit)
     (implicit tx: T, format: TFormat[T, A with Publisher[T, U]]): Observation[T, A] = {
    log.debug(s"observation for $value")
    val obs = value.changed.react(observe)
    new Observation[T, A](tx.newHandle(value), obs)
  }

  // def option[T <: Txn[T], U, A <: Publisher[T, U]](init: scala.Option[A] = None)(observe: T => U => Unit)
  
  //  trait Like[T <: Txn[T]] extends Disposable[T] {
  //    def observer: Disposable[T]
  //
  //    def dispose()(implicit tx: T): Unit = observer.dispose()
  //  }

  // type Option[T <: Txn[T], A] = Ref[OptionValue[T, A]]
  
  //  class Option[T <: Txn[T], A](val value: scala.Option[Source[T, A]], val observer: Disposable[T])
  //    extends Observation.Like[T]
}
class Observation[T <: Txn[T], A](val value: Source[T, A], val observer: Disposable[T])
  extends Disposable[T] {

  def dispose()(implicit tx: T): Unit = observer.dispose()

  override def toString = s"Observation@${hashCode.toHexString}($value, $observer)"
}