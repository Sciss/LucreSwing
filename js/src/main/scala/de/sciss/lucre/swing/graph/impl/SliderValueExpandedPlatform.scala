package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.Txn
import com.raquo.laminar.api.L._
import org.scalajs.dom

trait SliderValueExpandedPlatform[T <: Txn[T]] {
  protected def view: Slider.Repr[T]

  protected def viewUpdated(): Unit

  protected def viewState: Int = try {
    view.slider.ref.value.toInt
  } catch {
    case _: Exception => 0
  }

  protected def guiInit(): Unit = {
    val c: Input = view.slider
    val obs = Observer[dom.Event] { _ =>
      viewUpdated()
    }
    c.amend(
      onChange --> obs
    )
  }

  protected def guiDispose(): Unit = ()
}
