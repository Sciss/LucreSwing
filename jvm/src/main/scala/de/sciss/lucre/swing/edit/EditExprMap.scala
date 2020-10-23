/*
 *  EditExprMap.scala
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

import de.sciss.lucre.{Cursor, Expr, MapObj, Txn}
import javax.swing.undo.UndoableEdit

object EditExprMap {
  @deprecated("Try to transition to stm.UndoManager", since = "1.17.0")
  def apply[T <: Txn[T], K, A, Ex[~ <: Txn[~]] <: Expr[~, A]](name: String, map: MapObj.Modifiable[T, K, Ex],
                               key: K, value: Option[Ex[T]])
                              (implicit tx: T, cursor: Cursor[T],
                               keyType  : MapObj.Key[K],
                               valueType: Expr.Type[A, Ex]): UndoableEdit = {
    val before = map.get(key)
    val now: Option[Ex[T]] = (before, value) match {
      case (Some(valueType.Var(vr)), Some(v)) => return EditVar.Expr[T, A, Ex](name, vr, v) // current value is variable
      case (_, None) | (_, Some(valueType.Var(_))) => value  // new value is none or some variable, just put it
      case _ => value.map(v => valueType.newVar(v))          // new value is some non-variable, wrap it, then put it
    }
    import valueType.format
    val mapH      = tx.newHandle(map) // (MapObj.Modifiable.format[T, A, Expr[T, B], Change[B]])
    val beforeH   = tx.newHandle(before)
    val nowH      = tx.newHandle(now)
    val res       = new EditMutableMap.Impl(name, key, mapH, beforeH, nowH)
    res.perform()
    res
  }
}
