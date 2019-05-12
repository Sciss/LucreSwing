/*
 *  View.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import de.sciss.desktop.UndoManager
import de.sciss.lucre.stm
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder

import scala.swing.Component

object View {
  type T[S <: Sys[S], C1 <: Component] = View[S] { type C = C1 }

  trait Cursor[S <: Sys[S]] extends View[S] {
    implicit def cursor: stm.Cursor[S]
  }

  trait Editable[S <: Sys[S]] extends Cursor[S] {
    def undoManager: UndoManager
  }

  trait File {
    def file: java.io.File
  }

  def wrap[S <: Sys[S], C <: Component](component: => C)(implicit tx: S#Tx): View.T[S, C] = {
    val res = new Wrap[S, C]
    deferTx {
      res.guiInit(component)
    }
    res
  }

  private final class Wrap[S <: Sys[S], C1 <: Component] extends View[S] with ComponentHolder[C1] {
    type C = C1

    def guiInit(c: C1): Unit = component = c

    def dispose()(implicit tx: S#Tx): Unit = ()
  }
}
trait View[S <: Sys[S]] extends Disposable[S#Tx] {
  type C <: Component

  def component: C
}
