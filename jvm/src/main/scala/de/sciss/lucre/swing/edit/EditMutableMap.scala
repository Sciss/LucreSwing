/*
 *  EditMutableMap.scala
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

package de.sciss.lucre.swing.edit

import de.sciss.lucre.{Cursor, Elem, MapObj, Source, Txn}
import de.sciss.serial.TFormat
import javax.swing.undo.{AbstractUndoableEdit, UndoableEdit}

object EditMutableMap {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[T <: Txn[T], K, Ex[~ <: Txn[~]] <: Elem[~]](name: String, map: MapObj.Modifiable[T, K, Ex],
                                         key: K, value: Option[Ex[T]])
                                        (implicit tx: T, cursor: Cursor[T],
                                         keyType: MapObj.Key[K],
                                         valueFormat: TFormat[T, Ex[T]]): UndoableEdit = {
    val before = map.get(key)

    val mapH      = tx.newHandle(map) // (MapObj.Modifiable.format[T, K, Ex])
    val beforeH   = tx.newHandle(before)
    val nowH      = tx.newHandle(value)
    val res       = new Impl(name, key, mapH, beforeH, nowH)
    res.perform()
    res
  }

  private[edit] final class Impl[T <: Txn[T], A, Repr[~ <: Txn[~]] <: Elem[~], U](name: String, key: A,
                                                       mapH   : Source[T, MapObj.Modifiable[T, A, Repr]],
                                                       beforeH: Source[T, Option[Repr[T]]],
                                                       nowH   : Source[T, Option[Repr[T]]])(implicit cursor: Cursor[T])
    extends AbstractUndoableEdit {

    type B = Repr[T]

    override def undo(): Unit = {
      super.undo()
      cursor.step { implicit tx => perform(beforeH) }
    }

    override def redo(): Unit = {
      super.redo()
      cursor.step { implicit tx => perform() }
    }

    private def perform(exprH: Source[T, Option[B]])(implicit tx: T): Unit = {
      val map = mapH()
      exprH().fold {
        map.remove(key)
      } { expr =>
        map.put(key, expr)
      }
    }

    def perform()(implicit tx: T): Unit = perform(nowH)

    override def getPresentationName: String = name
  }
}
