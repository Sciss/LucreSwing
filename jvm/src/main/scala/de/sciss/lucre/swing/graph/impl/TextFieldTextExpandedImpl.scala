package de.sciss.lucre.swing
package graph
package impl

import java.awt.event.{ActionEvent, ActionListener, FocusEvent, FocusListener}

import de.sciss.lucre.{Cursor, ITargets, Txn}

final class TextFieldTextExpandedImpl[T <: Txn[T]](peer: => View.TextField, value0: String)
                                                  (implicit targets: ITargets[T], cursor: Cursor[T])
  extends ComponentPropertyExpandedImpl[T, String](value0) {

  private[this] lazy val listenerA = new ActionListener {
    def actionPerformed(e: ActionEvent): Unit = commit()
  }

  private[this] lazy val listenerF = new FocusListener {
    def focusLost(e: FocusEvent): Unit = commit()

    def focusGained(e: FocusEvent): Unit = ()
  }

  protected def valueOnEDT: String = peer.text

  protected def startListening(): Unit = {
    val c = peer
    val p = c.peer
    p.addActionListener (listenerA)
    p.addFocusListener  (listenerF)
  }

  protected def stopListening(): Unit = {
    val c = peer
    val p = c.peer
    p.removeActionListener(listenerA)
    p.removeFocusListener (listenerF)
  }
}
