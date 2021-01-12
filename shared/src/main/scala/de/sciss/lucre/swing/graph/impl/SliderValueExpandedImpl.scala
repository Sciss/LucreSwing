/*
 *  SliderValueExpandedImpl.scala
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

final class SliderValueExpandedImpl[T <: Txn[T]](view: Slider.Repr[T], value0: Int)
                                                  (implicit targets: ITargets[T], cursor: Cursor[T])
  extends NumberValueExpandedImpl[T, Int, Slider.Repr](view, value0)
    with SliderValueExpandedPlatform[T]
