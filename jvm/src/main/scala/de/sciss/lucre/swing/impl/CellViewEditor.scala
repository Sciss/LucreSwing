/*
 *  CellViewEditor.scala
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

package de.sciss.lucre.swing.impl

import de.sciss.lucre.swing.View
import de.sciss.lucre.{Disposable, Txn}
import javax.swing.event.{UndoableEditEvent, UndoableEditListener}

import scala.swing.{Component, TextComponent}

trait CellViewEditor[T <: Txn[T], A, Comp <: Component]
  extends View[T] with ComponentHolder[Comp] with CellViewFactory.View[A] {

  type C = Comp

  // ---- abstract ----

  // the current ephemeral (but committed) value of the view. sub classes should
  // implement this with the correct initial value
  protected var value: A

  // must be implemented by updating the GUI component with the current `value`
  protected def valueToComponent(): Unit

  // must be implemented by creating the GUI component
  protected def createComponent(): Comp

  // final var observer: Disposable[T] = _
  protected def observer: Disposable[T]

  // maybe be set by the sub class in `createComponent()`
  final protected var dirty = Option.empty[DirtyBorder]

  // clears the dirty status if `dirty` is non empty
  protected def clearDirty(): Unit = dirty.foreach(_.visible = false)

  // called when the expression changes, so that the change will be reflected in the GUI
  final def update(newValue: A): Unit =
    if (value != newValue) {
      value = newValue
      valueToComponent()
      clearDirty()
    }

  // installs an edit listener for the given text component which will flag `dirty` upon the first change
  // to the component's document
  final protected def observeDirty(text: TextComponent): Unit = {
    // the fucking JSpinner implementation removes and re-inserts its text when focused,
    // at least with aqua LaF. this means that two undoable edits are fired which are
    // completely pointless and idiotically marked as "significant". In order to skip
    // them, we register focus and key listeners.
    val j = text.peer
    // var valid = true // false
    j.getDocument.addUndoableEditListener(new UndoableEditListener {
      def undoableEditHappened(e: UndoableEditEvent): Unit = {
        // if (valid) {
        // println(s"UNDOABLE EDIT: ${e.getEdit}")
        dirty.foreach(_.visible = true)
        // }
      }
    })
    //    text.peer.addFocusListener(new FocusListener {
    //      def focusLost  (e: FocusEvent) = ()
    //      def focusGained(e: FocusEvent): Unit = valid = false
    //    })
    //    text.peer.addKeyListener(new KeyListener {
    //      def keyReleased(e: KeyEvent): Unit = ()
    //      def keyPressed (e: KeyEvent): Unit = valid = true
    //      def keyTyped   (e: KeyEvent): Unit = ()
    //    })
  }

  final def guiInit(): Unit =
    component = createComponent()

  // disposes the observer.
  def dispose()(implicit tx: T): Unit = observer.dispose()
}
