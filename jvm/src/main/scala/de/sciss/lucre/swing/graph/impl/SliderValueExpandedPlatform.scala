package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.Txn
import javax.swing.event.{ChangeEvent, ChangeListener}

trait SliderValueExpandedPlatform[T <: Txn[T]] {
  protected def view: Slider.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Int = view.slider.value

  private[this] lazy val listener = new ChangeListener {
    def stateChanged(e: ChangeEvent): Unit = viewUpdated()
  }

  protected def guiInit(): Unit = {
    val c = view.slider
    c.peer.addChangeListener(listener)
  }

  protected def guiDispose(): Unit = {
    val c = view.slider
    c.peer.removeChangeListener(listener)
  }
}
