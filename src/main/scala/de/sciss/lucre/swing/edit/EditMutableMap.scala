/*
 *  EditMutableMap.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre
package swing
package edit

import javax.swing.undo.{AbstractUndoableEdit, UndoableEdit}

import de.sciss.lucre.{event => evt}
import de.sciss.lucre.stm.{Elem, Sys}
import de.sciss.serial.Serializer

import scala.language.higherKinds

object EditMutableMap {
  def apply[S <: Sys[S], K, Ex[~ <: Sys[~]] <: Elem[~]](name: String, map: evt.Map.Modifiable[S, K, Ex],
                                         key: K, value: Option[Ex[S]])
                                        (implicit tx: S#Tx, cursor: stm.Cursor[S],
                                         keyType: evt.Map.Key[K],
                                         valueSerializer: Serializer[S#Tx, S#Acc, Ex[S]]): UndoableEdit = {
    val before = map.get(key)

    val mapH      = tx.newHandle(map) // (evt.Map.Modifiable.serializer[S, K, Ex])
    val beforeH   = tx.newHandle(before)
    val nowH      = tx.newHandle(value)
    val res       = new Impl(name, key, mapH, beforeH, nowH)
    res.perform()
    res
  }

  private[edit] final class Impl[S <: Sys[S], A, Repr[~ <: Sys[~]] <: Elem[~], U](name: String, key: A,
                                                       mapH   : stm.Source[S#Tx, evt.Map.Modifiable[S, A, Repr]],
                                                       beforeH: stm.Source[S#Tx, Option[Repr[S]]],
                                                       nowH   : stm.Source[S#Tx, Option[Repr[S]]])(implicit cursor: stm.Cursor[S])
    extends AbstractUndoableEdit {

    type B = Repr[S]

    override def undo(): Unit = {
      super.undo()
      cursor.step { implicit tx => perform(beforeH) }
    }

    override def redo(): Unit = {
      super.redo()
      cursor.step { implicit tx => perform() }
    }

    private def perform(exprH: stm.Source[S#Tx, Option[B]])(implicit tx: S#Tx): Unit = {
      val map = mapH()
      exprH().fold {
        map.remove(key)
      } { expr =>
        map.put(key, expr)
      }
    }

    def perform()(implicit tx: S#Tx): Unit = perform(nowH)

    override def getPresentationName = name
  }
}
