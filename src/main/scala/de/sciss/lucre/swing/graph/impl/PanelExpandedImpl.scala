/*
 *  PanelExpandedImpl.scala
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

package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.expr.Context
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.Panel

trait PanelExpandedImpl[S <: Sys[S]] extends ComponentExpandedImpl[S] {

  override def initComponent()(implicit tx: S#Tx, ctx: Context[S]): this.type = {
    initProperty(Panel.keyBorder, Panel.defaultBorder) {
      c => if (c != Panel.defaultBorder) component.border = c.mkPeer()
    }
    super.initComponent()
  }
}
