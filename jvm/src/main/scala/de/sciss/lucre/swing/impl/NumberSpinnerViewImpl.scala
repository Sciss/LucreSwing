/*
 *  NumberSpinnerViewImpl.scala
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

import java.awt.Color
import java.awt.event.KeyEvent
import java.text.NumberFormat
import java.util.Locale

import de.sciss.audiowidgets
import de.sciss.desktop.UndoManager
import de.sciss.lucre.{Cursor, Disposable, Txn}
import de.sciss.swingplus.Spinner
import javax.swing.JFormattedTextField.AbstractFormatter
import javax.swing.text.DefaultFormatterFactory
import javax.swing.{JFormattedTextField, JSpinner, KeyStroke, SpinnerModel, SwingConstants}

import scala.swing.event.{FocusLost, KeyTyped, ValueChanged}
import scala.swing.{Action, Component, Swing, TextComponent}

trait NumberSpinnerViewImpl[T <: Txn[T], A] // (implicit cursor: Cursor[T], undoManager: UndoManager)
  extends CellViewEditor[T, A, Spinner] {

  protected def maxWidth: Int

  protected def cursor: Cursor[T]
  protected def undoManager: UndoManager

  // current display value (GUI threaded)
  protected var value: A

  // reactive observer (will be disposed with component)
  protected def observer: Disposable[T]

  // final protected val tpe = expr.Int

  protected def committer: Option[CellViewFactory.Committer[T, A]]

  private var sp: Spinner = _

  protected def model: SpinnerModel // SpinnerNumberModel

  protected def parseModelValue(v: Any): Option[A]

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
  
  protected def mkSpinner: Spinner = new Spinner(model) {
    override lazy val peer: javax.swing.JSpinner = new javax.swing.JSpinner(model) with SuperMixin {
      override def getLocale: Locale = Locale.US  // this is used for the decimal format!

      // bug with aqua look and feel. JSpinner relies on getComponent(0),
      // which might not have a baseline. Fall back to editor's baseline then.
      override def getBaseline(width: Int, height: Int): Int = {
        val res = super.getBaseline(width, height)
        if (res >= 0) res else {
          getEditor.getBaseline(width, height)
        }
      }
    }
  }

  final protected def createComponent(): Spinner = {
    sp        = mkSpinner
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
          override lazy val peer: JFormattedTextField = e.getTextField
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

abstract class DefinedNumberSpinnerViewImpl[T <: Txn[T], A](protected val maxWidth: Int)
                                                           (implicit protected val cursor: Cursor[T],
                                                            protected val undoManager: UndoManager)
  extends NumberSpinnerViewImpl[T, A] {

  final protected def valueToComponent(): Unit =
    if (component.value != value) {
      // println("valueToComponent()")
      component.value = value
    }
}

abstract class OptionalNumberSpinnerViewImpl[T <: Txn[T], A](protected val maxWidth: Int, isInteger: Boolean)
                                                            (implicit protected val cursor: Cursor[T],
                                                             protected val undoManager: UndoManager)
  extends NumberSpinnerViewImpl[T, Option[A]] {

  def this(maxWidth: Int)(implicit cursor: Cursor[T], undoManager: UndoManager) =
    this(maxWidth, false)

  protected def default: Option[A]

  override protected def model: NumericOptionSpinnerModel[A]

  override protected def mkSpinner: Spinner = {
    val res     = super.mkSpinner
    val ftf     = res.peer.getEditor.asInstanceOf[JSpinner.DefaultEditor].getTextField
    val fgNorm  = ftf.getForeground
    val fgDef   = if (audiowidgets.Util.isDarkSkin) new Color(0x5E * 2/3, 0x97 * 2/3, 0xFF) else Color.blue
    val fmt: AbstractFormatter = new AbstractFormatter {
      private val dec = if (isInteger) {
        val res = NumberFormat.getIntegerInstance(Locale.US)
        res.setGroupingUsed(false)
        res
      } else {
        val res = NumberFormat.getNumberInstance(Locale.US)
        res.setMaximumFractionDigits(5)
        res.setGroupingUsed(false)
        res
      }

      def valueToString(value: Any): String = {
        value match {
          case Some(d: Double) =>
            ftf.setForeground(fgNorm)
            dec.format(d)
          case Some(i: Int) =>
            ftf.setForeground(fgNorm)
            dec.format(i)
          case _ =>
            ftf.setForeground(fgDef)
            default.fold("")(dec.format)
        }
      }

      def stringToValue(text: String): AnyRef = {
        val t = text.trim
        if (t.isEmpty) None else {
          val num   = dec.parse(t)
          // N.B.!! Scala will widen `num.intValue()` to `Double`
          // in an attempt to give a smart upper bound, unless we explicitly type `: Any`!
          val value: Any = if (isInteger) num.intValue() else num.doubleValue()
          Some(value)
        }
      }
    }
    val factory = new DefaultFormatterFactory(fmt, fmt, fmt)
    ftf.setEditable(true)
    ftf.setFormatterFactory(factory)
    ftf.setHorizontalAlignment(SwingConstants.RIGHT)
    val maxString = fmt.valueToString(model.maximum)
    val minString = fmt.valueToString(model.minimum)
    ftf.setColumns(math.max(maxString.length, minString.length))
    res
  }
}