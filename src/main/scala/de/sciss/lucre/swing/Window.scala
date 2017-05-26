/*
 *  Window.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2017 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import javax.swing.{RootPaneContainer, SwingUtilities}

import de.sciss.desktop
import de.sciss.lucre.stm.{Disposable, Sys}

import scala.swing.Component

object Window {
  private[lucre] final val Property = "de.sciss.lucre.swing.Window"

  def attach[S <: Sys[S]](dw: desktop.Window, l: Window[S]): Unit = {
    requireEDT()
    val rp = dw.component.peer.getRootPane
    rp.putClientProperty(Property, l)
  }

  def find[S <: Sys[S]](view: View[S]): Option[Window[S]] = findFor(view.component)

  def findFor[S <: Sys[S]](component: Component): Option[Window[S]] = {
    requireEDT()
    val rpc = SwingUtilities.getAncestorOfClass(classOf[RootPaneContainer], component.peer)
    if (rpc == null) return None
    val rp  = rpc.asInstanceOf[RootPaneContainer].getRootPane
    val w   = rp.getClientProperty(Property)
    if (w == null) return None
    Some(w.asInstanceOf[Window[S]])
  }
}
trait Window[S <: Sys[S]] extends Disposable[S#Tx] {
  def window: desktop.Window
  def view: View[S]
}