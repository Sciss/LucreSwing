/*
 *  View.scala
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

import de.sciss.desktop.UndoManager
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Disposable, Txn, Cursor => LCursor}

object View {
  type T[Tx <: Txn[Tx], C1 <: Component] = View[Tx] { type C = C1 }

  type Component    = scala.swing.Component
  type Button       = scala.swing.Button
  type CheckBox     = scala.swing.CheckBox
  type Slider       = scala.swing.Slider
  type IntField     = de.sciss.audiowidgets.ParamField[Int]
  type DoubleField  = de.sciss.audiowidgets.ParamField[Double]
  type ComboBox[A]  = de.sciss.swingplus.ComboBox[A]
  type TextField    = scala.swing.TextField

  trait Cursor[Tx <: Txn[Tx]] extends View[Tx] {
    implicit def cursor: LCursor[Tx]
  }

  trait Editable[Tx <: Txn[Tx]] extends Cursor[Tx] {
    def undoManager: UndoManager
  }

  trait File {
    def file: java.io.File
  }

  def wrap[Tx <: Txn[Tx], C <: Component](component: => C)(implicit tx: Tx): View.T[Tx, C] = {
    val res = new Wrap[Tx, C]
    deferTx {
      res.guiInit(component)
    }
    res
  }

  private final class Wrap[Tx <: Txn[Tx], C1 <: Component] extends View[Tx] with ComponentHolder[C1] {
    type C = C1

    def guiInit(c: C1): Unit = component = c

    def dispose()(implicit tx: Tx): Unit = ()
  }
}
trait View[T <: Txn[T]] extends Disposable[T] {
  type C <: View.Component

  def component: C
}
