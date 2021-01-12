/*
 *  ListView.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.swing.impl.ListViewImpl
import de.sciss.lucre.{Disposable, ListObj, Txn}
import de.sciss.model.Model
import de.sciss.serial.TFormat
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
    def apply[T <: Txn[T], Elem, U](dataFun  : T =>  Elem     =>        String )
                                   (updateFun: T => (Elem, U) => Option[String])
    : Handler[T, Elem, U, String] = new Handler[T, Elem, U, String] {

      def renderer(view: ListView[T, Elem, U], data: String, index: Int): Component = ???

      def data      (elem: Elem           )(implicit tx: T):        String  = dataFun  (tx)(elem        )
      def dataUpdate(elem: Elem, update: U)(implicit tx: T): Option[String] = updateFun(tx)(elem, update)
    }

    def apply[T <: Txn[T], Elem](dataFun: T => Elem => String): Handler[T, Elem, Unit, String] =
      apply[T, Elem, Unit](dataFun)(_ => (_, _) => None)
  }

  trait Handler[T <: Txn[T], Elem, U, Data] {
    /** Called to generate non-transactional rendering data from an element. */
    def data(elem: Elem)(implicit tx: T): Data

    /** Called to generate non-transactional rendering data from an element when it is updated. */
    def dataUpdate(elem: Elem, update: U)(implicit tx: T): Option[Data]

    def renderer(view: ListView[T, Elem, U], data: Data, index: Int
                 /* state: TreeTableCellRenderer.State */): Component
  }

  def apply[T <: Txn[T], Elem, U, Data](list: ListObj[T, Elem], handler: Handler[T, Elem, U, Data])
                                       (implicit tx: T, format: TFormat[T, ListObj[T, Elem]])
  : ListView[T, Elem, U] = ListViewImpl(list, handler)

  def empty[T <: Txn[T], Elem, U, Data](handler: Handler[T, Elem, U, Data])
                                       (implicit tx: T,
                                        format: TFormat[T, ListObj[T, Elem]])
  : ListView[T, Elem, U] = ListViewImpl.empty(handler)

  sealed trait Update
  final case class SelectionChanged(current: Vec[Int]) extends Update
}
trait ListView[T <: Txn[T], Elem, U] extends Disposable[T] with Model[ListView.Update] {
  def component: Component
  def view: swingplus.ListView[_]

  def guiSelection: Vec[Int]

  def list                               (implicit tx: T): Option[ListObj[T, Elem]]
  def list_=(list: Option[ListObj[T, Elem]])(implicit tx: T): Unit
}
