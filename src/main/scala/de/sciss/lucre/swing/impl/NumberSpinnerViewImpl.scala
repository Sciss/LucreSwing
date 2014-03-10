package de.sciss.lucre.swing
package impl

import de.sciss.lucre.event.Sys
import de.sciss.lucre.{expr, stm}
import de.sciss.desktop.UndoManager
import de.sciss.swingplus.Spinner
import de.sciss.lucre.stm.Disposable
import javax.swing.{JSpinner, SpinnerNumberModel}
import scala.swing.TextComponent
import scala.swing.event.ValueChanged

abstract class NumberSpinnerViewImpl[S <: Sys[S], A](maxWidth: Int)
                                                    (implicit cursor: stm.Cursor[S], undoManager: UndoManager)
  extends ExprEditor[S, A, Spinner] {

  // current display value (GUI threaded)
  protected var value: A

  // reactive observer (will be disposed with component)
  protected def observer: Disposable[S#Tx]

  // final protected val tpe = expr.Int

  protected def committer: Option[ExprViewFactory.Committer[S, A]]

  final protected def valueToComponent(): Unit =
    if (sp.value != value) {
      // println("valueToComponent()")
      sp.value = value
    }

  private var sp: Spinner = _

  protected def model: SpinnerNumberModel

  protected def parseModelValue(v: Any): Option[A]

  final protected def createComponent(): Spinner = {
    // val spm   = new SpinnerNumberModel(value, Int.MinValue, Int.MaxValue, 1)
    sp        = new Spinner(model)
    val d1    = sp.preferredSize
    d1.width  = math.min(d1.width, maxWidth) // XXX TODO WTF
    sp.preferredSize = d1
    val d2    = sp.maximumSize
    d2.width  = math.min(d2.width, maxWidth)
    sp.maximumSize   = d2
    val d3    = sp.minimumSize
    d3.width  = math.min(d3.width, maxWidth)
    sp.minimumSize = d3
    sp.peer.getEditor match {
      case e: JSpinner.DefaultEditor =>
        val txt = new TextComponent { override lazy val peer = e.getTextField }
        dirty   = Some(DirtyBorder(txt))
      // THIS SHIT JUST DOESN'T WORK, FUCK YOU SWING
      // observeDirty(txt)
      case _ =>
    }

    committer.foreach { com =>
      sp.listenTo(sp)
      sp.reactions += {
        case ValueChanged(_) =>
          parseModelValue(sp.value).foreach { newValue =>
            if (value != newValue) cursor.step { implicit tx =>
              val edit = com.commit(newValue)
              undoManager.add(edit)
              value = newValue
            }
            clearDirty()
          }
      }
    }

    sp
  }
}