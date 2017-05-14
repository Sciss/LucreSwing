/*
 *  ListView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.expr.List
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.impl.{ListViewImpl => Impl}
import de.sciss.model.Model
import de.sciss.serial.Serializer
import de.sciss.swingplus

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.Component

object ListView {
  object Handler {
    /** Creates a simple handler which produces string representations and a standard list item renderer.
      *
      * @param dataFun    the function which generates a string representation of a list element
      * @param updateFun  the function which generates a string from an element update
      */
    def apply[S <: Sys[S], Elem, U](dataFun  : S#Tx =>  Elem     =>        String )
                                   (updateFun: S#Tx => (Elem, U) => Option[String])
    : Handler[S, Elem, U, String] = new Handler[S, Elem, U, String] {

      def renderer(view: ListView[S, Elem, U], data: String, index: Int): Component = ???

      def data      (elem: Elem           )(implicit tx: S#Tx):        String  = dataFun  (tx)(elem        )
      def dataUpdate(elem: Elem, update: U)(implicit tx: S#Tx): Option[String] = updateFun(tx)(elem, update)
    }

    def apply[S <: Sys[S], Elem](dataFun: S#Tx => Elem => String): Handler[S, Elem, Unit, String] =
      apply[S, Elem, Unit](dataFun)(_ => (_, _) => None)
  }

  trait Handler[S <: Sys[S], Elem, U, Data] {
    /** Called to generate non-transactional rendering data from an element. */
    def data(elem: Elem)(implicit tx: S#Tx): Data

    /** Called to generate non-transactional rendering data from an element when it is updated. */
    def dataUpdate(elem: Elem, update: U)(implicit tx: S#Tx): Option[Data]

    def renderer(view: ListView[S, Elem, U], data: Data, index: Int
                 /* state: TreeTableCellRenderer.State */): Component
  }

  def apply[S <: Sys[S], Elem, U, Data](list: List[S, Elem], handler: Handler[S, Elem, U, Data])
                                       (implicit tx: S#Tx, serializer: Serializer[S#Tx, S#Acc, List[S, Elem]])
  : ListView[S, Elem, U] = Impl(list, handler)

  def empty[S <: Sys[S], Elem, U, Data](handler: Handler[S, Elem, U, Data])
                                       (implicit tx: S#Tx,
                                        serializer: Serializer[S#Tx, S#Acc, List[S, Elem]])
  : ListView[S, Elem, U] = Impl.empty(handler)

  sealed trait Update
  final case class SelectionChanged(current: Vec[Int]) extends Update
}
trait ListView[S <: Sys[S], Elem, U] extends Disposable[S#Tx] with Model[ListView.Update] {
  def component: Component
  def view: swingplus.ListView[_]

  def guiSelection: Vec[Int]

  def list                               (implicit tx: S#Tx): Option[List[S, Elem]]
  def list_=(list: Option[List[S, Elem]])(implicit tx: S#Tx): Unit
}
