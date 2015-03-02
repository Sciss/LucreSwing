/*
 *  NumberSpinnerViewImpl.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2015 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package impl

import java.awt.event.KeyEvent
import javax.swing.{JSpinner, KeyStroke, SpinnerNumberModel}

import de.sciss.desktop.UndoManager
import de.sciss.lucre.event.Sys
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Disposable
import de.sciss.swingplus.Spinner

import scala.swing.event.{FocusLost, KeyTyped, ValueChanged}
import scala.swing.{Action, Component, Swing, TextComponent}

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

  // private var textValue: String = _

  private class TextObserver(sp: Component, peer: TextComponent) {
    private var textCommitted = peer.text
    private var textCurrent   = textCommitted
    private var isClean       = true
    private val db            = DirtyBorder(peer)

    dirty   = Some(db)

    peer.listenTo(peer)
    peer.listenTo(peer.keys)
    peer.reactions += {
      case FocusLost(_, _, _) if !isClean =>
        textCurrent = textCommitted
        isClean     = true
        db.visible  = false

      case KeyTyped(_, _, _, _) if isClean =>
        Swing.onEDT {
          textCurrent = peer.text
          isClean     = textCommitted == textCurrent
          if (!isClean) db.visible = true
        }
    }

    {
      val keyAbort  = "de.sciss.Abort"
      val aMap      = peer.peer.getActionMap
      val iMap      = peer.peer.getInputMap
      aMap.put(keyAbort, Action("Cancel Editing") { if (!isClean) cancel() } .peer)
      iMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), keyAbort)
    }

    def committed(): Unit = Swing.onEDT {
      textCommitted = peer.text
      textCurrent   = textCommitted
      isClean       = true
      db.visible    = false
    }

    def cancel(): Unit = {
      peer.text = textCommitted
      committed()
    }
  }

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

    val obsOpt = sp.peer.getEditor match {
      case e: JSpinner.DefaultEditor =>
        val ggText: TextComponent = new TextComponent {
          override lazy val peer = e.getTextField
        }
        Some(new TextObserver(sp, ggText))

      case _ => None: Option[TextObserver]
    }

    //        val txt = new TextComponent {
    //          override lazy val peer = e.getTextField
    //
    //          //          listenTo(this)
    //          //          reactions += {
    //          //            case EditDone(_) => println("EDIT DONE")
    //          //          }
    //        }
    //        // THIS SHIT JUST DOESN'T WORK, FUCK YOU SWING
    //        observeDirty(txt)
    //      case _ =>
    //    }


    committer.foreach { com =>
      sp.listenTo(sp)
      sp.reactions += {
        case ValueChanged(_) =>
          // println("VALUE CHANGED")
          parseModelValue(sp.value).foreach { newValue =>
            if (value != newValue) {
              val edit = cursor.step { implicit tx =>
                com.commit(newValue)
              }
              undoManager.add(edit)
              value = newValue
            }
            obsOpt.foreach(_.committed())
          }
      }
    }

    sp
  }
}