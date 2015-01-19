/*
 *  Window.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm.Disposable
import de.sciss.desktop

trait Window[S <: Sys[S]] extends Disposable[S#Tx] {
  def window: desktop.Window
  def view: View[S]
}