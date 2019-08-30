/*
 *  EditExprMap.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.edit

import de.sciss.lucre.expr.{Expr, Type}
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.{stm, event => evt}
import javax.swing.undo.UndoableEdit

import scala.language.higherKinds

object EditExprMap {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: Expr[~, A]](name: String, map: evt.Map.Modifiable[S, K, Ex],
                               key: K, value: Option[Ex[S]])
                              (implicit tx: S#Tx, cursor: stm.Cursor[S],
                               keyType  : evt.Map.Key[K],
                               valueType: Type.Expr[A, Ex]): UndoableEdit = {
    val before = map.get(key)
    val now: Option[Ex[S]] = (before, value) match {
      case (Some(valueType.Var(vr)), Some(v)) => return EditVar.Expr[S, A, Ex](name, vr, v) // current value is variable
      case (_, None) | (_, Some(valueType.Var(_))) => value  // new value is none or some variable, just put it
      case _ => value.map(v => valueType.newVar(v))          // new value is some non-variable, wrap it, then put it
    }

    import valueType.serializer
    val mapH      = tx.newHandle(map) // (evt.Map.Modifiable.serializer[S, A, Expr[S, B], Change[B]])
    val beforeH   = tx.newHandle(before)
    val nowH      = tx.newHandle(now)
    val res       = new EditMutableMap.Impl(name, key, mapH, beforeH, nowH)
    res.perform()
    res
  }
}
