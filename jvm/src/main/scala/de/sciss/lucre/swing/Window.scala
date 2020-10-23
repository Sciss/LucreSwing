/*
 *  Window.scala
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

import de.sciss.desktop
import de.sciss.lucre.{Disposable, Txn}
import de.sciss.lucre.swing.LucreSwing.requireEDT
import javax.swing.{RootPaneContainer, SwingUtilities}

import scala.swing.Component

object Window {
  private[lucre] final val Property = "de.sciss.lucre.swing.Window"

  def attach[T <: Txn[T]](dw: desktop.Window, l: Window[T]): Unit = {
    requireEDT()
    val rp = dw.component.peer.getRootPane
    rp.putClientProperty(Property, l)
  }

  def find[T <: Txn[T]](view: View[T]): Option[Window[T]] = findFor(view.component)

  def findFor[T <: Txn[T]](component: Component): Option[Window[T]] = {
    requireEDT()
    val rpc = SwingUtilities.getAncestorOfClass(classOf[RootPaneContainer], component.peer)
    if (rpc == null) return None
    val rp  = rpc.asInstanceOf[RootPaneContainer].getRootPane
    val w   = rp.getClientProperty(Property)
    if (w == null) return None
    Some(w.asInstanceOf[Window[T]])
  }
}
trait Window[T <: Txn[T]] extends Disposable[T] {
  def window: desktop.Window
  def view: View[T]
}