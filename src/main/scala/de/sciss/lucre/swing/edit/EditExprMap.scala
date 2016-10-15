/*
 *  EditExprMap.scala
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

import javax.swing.undo.UndoableEdit

import de.sciss.lucre.expr.Type
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.{event => evt}

import scala.language.higherKinds

object EditExprMap {
  def apply[S <: Sys[S], K, A, Ex[~ <: Sys[~]] <: expr.Expr[~, A]](name: String, map: evt.Map.Modifiable[S, K, Ex],
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
