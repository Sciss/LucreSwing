package de.sciss.lucre.swing
package graph
package impl

import java.awt.event.{ActionEvent, ActionListener}

import de.sciss.lucre.Txn

trait CheckBoxSelectedExpandedPlatform[T <: Txn[T]] {
  protected def view: CheckBox.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Boolean = view.checkBox.selected

  private[this] lazy val listener = new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = viewUpdated()
  }

  protected def guiInit(): Unit = {
    val c = view.checkBox
    c.peer.addActionListener(listener)
  }

  protected def guiDispose(): Unit = {
    val c = view.checkBox
    c.peer.removeActionListener(listener)
  }
}
