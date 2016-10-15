/*
 *  View.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2016 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Disposable
import de.sciss.lucre.swing.impl.ComponentHolder

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

  def wrap[S <: Sys[S]](component: => Component)(implicit tx: S#Tx): View[S] = {
    val res = new Wrap[S]
    deferTx {
      res.guiInit(component)
    }
    res
  }

  private final class Wrap[S <: Sys[S]] extends View[S] with ComponentHolder[Component] {
    def guiInit(c: Component): Unit = component = c

    def dispose()(implicit tx: S#Tx) = ()
  }
}
trait View[S <: Sys[S]] extends Disposable[S#Tx] {
  def component: Component
}
