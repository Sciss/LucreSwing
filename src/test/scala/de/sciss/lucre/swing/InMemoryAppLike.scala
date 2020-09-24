package de.sciss.lucre.swing

import de.sciss.lucre.edit.UndoManager
import de.sciss.lucre.{Cursor, InMemory, Workspace}

trait InMemoryAppLike extends AppLike {
  type S = InMemory
  type T = InMemory.Txn
  implicit val system: S with Cursor[T] = InMemory()

  implicit val undoT: UndoManager[T] = UndoManager()
  implicit lazy val workspace: Workspace[T] = Workspace.Implicits.dummy
}
