/*
 *  IntFieldValueExpandedImpl.scala
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
package graph
package impl

import de.sciss.lucre.{Cursor, ITargets, Txn}

final class IntFieldValueExpandedImpl[T <: Txn[T]](view: IntField.Repr[T], value0: Int)
                                              (implicit targets: ITargets[T], cursor: Cursor[T])
  extends NumberValueExpandedImpl[T, Int, IntField.Repr](view, value0)
    with IntFieldValueExpandedPlatform[T]
