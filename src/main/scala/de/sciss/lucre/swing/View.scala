/*
 *  View.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm
import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm.Disposable
import scala.swing.Component

object View {
  trait Cursor[S <: Sys[S]] extends View[S] {
    implicit def cursor: stm.Cursor[S]
  }

  trait Editable[S <: Sys[S]] extends Cursor[S] {
    def undoManager: UndoManager
  }

  trait File {
    def file: java.io.File
  }
}
trait View[S <: Sys[S]] extends Disposable[S#Tx] {
  def component: Component
}
