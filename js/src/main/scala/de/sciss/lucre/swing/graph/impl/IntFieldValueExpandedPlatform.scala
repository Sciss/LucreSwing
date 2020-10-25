/*
 *  IntFieldValueExpandedPlatform.scala
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
package graph
package impl

import com.raquo.laminar.api.L
import de.sciss.lucre.Txn

trait IntFieldValueExpandedPlatform[T <: Txn[T]] extends IntFieldLikeValueExpandedPlatform[T] {
  protected def view: IntField.Repr[T]

  protected def input: L.Input = view.intField
}
