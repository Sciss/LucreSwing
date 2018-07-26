/*
 *  PathField.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package graph

import de.sciss.desktop.FileDialog
import de.sciss.file.File
import de.sciss.lucre.aux.Aux
import de.sciss.lucre.event.impl.IGenerator
import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.expr.ExOps._
import de.sciss.lucre.expr.graph.Constant
import de.sciss.lucre.expr.{Ex, IExpr}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.Sys
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.model.Change

import scala.concurrent.stm.Ref
import scala.swing.event.ValueChanged

object PathField {
  //  def apply(text    : Ex[String ]): PathField = this(text, 0)
  //  def apply(columns : Ex[Int    ]): PathField = this("", columns)

  def apply(): PathField = Impl()

  def apply(mode: Ex[Int]): PathField = {
    val res   = apply()
    res.mode  = mode
    res
  }

  val Open  : Ex[Int] = 0
  val Save  : Ex[Int] = 1
  val Folder: Ex[Int] = 1

  private final val keyValue        = "value"
  private final val keyTitle        = "title"
  private final val keyMode         = "mode"
  private final val defaultValue    = new File("")
  private final val defaultMode     = 0

  final case class Path(w: PathField) extends Ex[File] {
    override def productPrefix: String = s"PathField$$Path" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, File] = ctx match {
      case b: Widget.Builder[S] =>
        import b.{cursor, targets}
        val ws        = w.expand[S](b, tx)
        val valueOpt  = b.getProperty[Ex[File]](w, keyValue)
        val value0    = valueOpt.fold[File](defaultValue)(_.expand[S].value)
        new PathExpanded[S](ws, value0).init()
    }

    def aux: List[Aux] = Nil
  }

  private final class PathExpanded[S <: Sys[S]](ws: View.T[S, de.sciss.desktop.PathField], value0: File)
                                               (implicit protected val targets: ITargets[S], cursor: stm.Cursor[S])
    extends IExpr[S, File]
      with IGenerator[S, Change[File]] {

    private def commit(): Unit = {
      val c       = ws.component
      val before  = guiValue
      val now     = c.value
      val ch      = Change(before, now)
      if (ch.isSignificant) {
        guiValue    = now
        cursor.step { implicit tx =>
          txValue.set(now)(tx.peer)
          fire(ch)
        }
      }
    }

    private[this] var guiValue: File = _
    private[this] val txValue = Ref(value0)

    def value(implicit tx: S#Tx): File = txValue.get(tx.peer)

    def changed: IEvent[S, Change[File]] = this

    private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[File]] =
      Some(pull.resolve[Change[File]])

    def init()(implicit tx: S#Tx): this.type = {
      deferTx {
        val c = ws.component
        c.listenTo(c)
        c.reactions += {
          case ValueChanged(_) => commit()
        }
        guiValue = c.value
      }
      this
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      deferTx {
        val c = ws.component
        c.deafTo(c)
      }
    }
  }

  private final val titleSeq = List(
    "Open File",
    "Save File",
    "Choose Folder"
  )

  private def defaultTitle(mode: Ex[Int]): Ex[String] = {
    Constant(titleSeq).applyOption(mode).getOrElse("Choose")
  }

  final case class Title(w: PathField) extends Ex[String] {
    override def productPrefix: String = s"PathField$$Title" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, String] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[String]](w, keyTitle)
        valueOpt.getOrElse(defaultTitle(w.mode)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  final case class Mode(w: PathField) extends Ex[Int] {
    override def productPrefix: String = s"PathField$$Mode" // serialization

    def expand[S <: Sys[S]](implicit ctx: Ex.Context[S], tx: S#Tx): IExpr[S, Int] = ctx match {
      case b: Widget.Builder[S] =>
        val valueOpt = b.getProperty[Ex[Int]](w, keyMode)
        valueOpt.getOrElse(Constant(defaultMode)).expand[S]
    }

    def aux: List[Aux] = Nil
  }

  private final class Expanded[S <: Sys[S]](protected val w: PathField) extends View[S]
    with ComponentHolder[de.sciss.desktop.PathField] with ComponentExpandedImpl[S] {

    type C = de.sciss.desktop.PathField

    override def init()(implicit tx: S#Tx, b: Widget.Builder[S]): this.type = {
      val valueOpt  = b.getProperty[Ex[File   ]](w, keyValue).map(_.expand[S].value)
      val titleOpt  = b.getProperty[Ex[String ]](w, keyTitle).map(_.expand[S].value)
      val mode      = b.getProperty[Ex[Int    ]](w, keyMode ).fold(defaultMode)(_.expand[S].value) match {
        case 0 => FileDialog.Open
        case 1 => FileDialog.Save
        case 2 => FileDialog.Folder
        case _ => FileDialog.Open
      }

      deferTx {
        val c = new de.sciss.desktop.PathField
        valueOpt.foreach(c.value = _)
        titleOpt.foreach(c.title = _)
        c.mode    = mode
        component = c
      }
      super.init()
    }
  }

  private final case class Impl() extends PathField with ComponentImpl {
    override def productPrefix: String = "PathField" // serialization

    protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View.T[S, C] = {
      new Expanded[S](this).init()
    }

    def value: Ex[File] = Path(this)

    def value_=(value: Ex[File]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyValue, value)
    }

    def title: Ex[String] = Title(this)

    def title_=(value: Ex[String]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyTitle, value)
    }

    def mode: Ex[Int] = Mode(this)

    def mode_=(value: Ex[Int]): Unit = {
      val b = Graph.builder
      b.putProperty(this, keyMode, value)
    }
  }
}
trait PathField extends Component {
  type C = de.sciss.desktop.PathField

  var title : Ex[String]
  var mode  : Ex[Int]
  var value : Ex[File]
}
