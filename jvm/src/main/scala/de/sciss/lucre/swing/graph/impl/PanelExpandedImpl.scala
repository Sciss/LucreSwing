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

import de.sciss.lucre.Txn
import de.sciss.lucre.expr.Context
import de.sciss.lucre.swing.graph.{Border, Panel}

import scala.swing.Swing

trait PanelExpandedImpl[T <: Txn[T]] extends ComponentExpandedImpl[T] {

  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
    initProperty(Panel.keyBorder, Panel.defaultBorder) {
      c => if (c != Panel.defaultBorder) c match {
        case e: Border.Empty =>
          component.border = Swing.EmptyBorder(top = e.top, left = e.left, bottom = e.bottom, right = e.right)
      }
    }
    super.initComponent()
  }
}
