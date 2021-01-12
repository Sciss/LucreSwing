/*
 *  PanelExpandedImpl.scala
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

package de.sciss.lucre.swing.graph.impl

import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.graph.{Border, Panel}

trait PanelExpandedImpl[T <: Txn[T]] extends ComponentExpandedImpl[T] {
//  type C <: Div

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    initProperty(Panel.keyBorder, Panel.defaultBorder) {
      c => if (c != Panel.defaultBorder) c match {
        case e: Border.Empty =>
          component.amend(
            padding := s"${e.top}px ${e.right}px ${e.bottom}px ${e.left}px ;"
          )
      }
    }
    super.initComponent()
  }
}
