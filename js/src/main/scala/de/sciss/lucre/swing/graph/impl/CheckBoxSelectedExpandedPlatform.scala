package de.sciss.lucre.swing
package graph
package impl

import com.raquo.laminar.api.L._
import de.sciss.lucre.Txn
import org.scalajs.dom

trait CheckBoxSelectedExpandedPlatform[T <: Txn[T]] { self =>
  protected def view: CheckBox.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Boolean = view.checkBox.ref.checked

  protected def guiInit(): Unit = {
    val c: Input = view.checkBox
    val obs = Observer[dom.MouseEvent] { _ =>
      viewUpdated()
    }
    c.amend(
      onClick --> obs
    )
  }

  protected def guiDispose(): Unit = ()
}
