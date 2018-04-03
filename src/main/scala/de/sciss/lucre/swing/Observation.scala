/*
 *  Observation.scala
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

import de.sciss.lucre.event.Publisher
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Sys, Disposable}
import de.sciss.serial.Serializer

object Observation {
  def apply[S <: Sys[S], U, A](value: A with Publisher[S, U])(
      observe: S#Tx => U => Unit)
     (implicit tx: S#Tx, serializer: Serializer[S#Tx, S#Acc, A with Publisher[S, U]]): Observation[S, A] = {
    log(s"observation for $value")
    val obs = value.changed.react(observe)
    new Observation[S, A](tx.newHandle(value), obs)
  }

  // def option[S <: Sys[S], U, A <: Publisher[S, U]](init: scala.Option[A] = None)(observe: S#Tx => U => Unit)
  
  //  trait Like[S <: Sys[S]] extends Disposable[S#Tx] {
  //    def observer: Disposable[S#Tx]
  //
  //    def dispose()(implicit tx: S#Tx): Unit = observer.dispose()
  //  }

  // type Option[S <: Sys[S], A] = Ref[OptionValue[S, A]]
  
  //  class Option[S <: Sys[S], A](val value: scala.Option[stm.Source[S#Tx, A]], val observer: Disposable[S#Tx])
  //    extends Observation.Like[S]
}
class Observation[S <: Sys[S], A](val value: stm.Source[S#Tx, A], val observer: Disposable[S#Tx]) 
  extends Disposable[S#Tx] {

  def dispose()(implicit tx: S#Tx): Unit = observer.dispose()

  override def toString = s"Observation@${hashCode.toHexString}($value, $observer)"
}