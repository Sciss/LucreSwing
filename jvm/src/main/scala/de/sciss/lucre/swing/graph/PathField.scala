/*
 *  PathField.scala
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

package de.sciss.lucre.swing.graph

import de.sciss.desktop.{FileDialog, PathField => Peer}
import de.sciss.file.File
import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, Graph, IControl, Model}
import de.sciss.lucre.swing.LucreSwing.deferTx
import de.sciss.lucre.swing.View
import de.sciss.lucre.swing.graph.impl.{ComponentExpandedImpl, ComponentImpl, PathFieldValueExpandedImpl}
import de.sciss.lucre.swing.impl.ComponentHolder
import de.sciss.lucre.{Artifact, IExpr, Txn}

import scala.util.Try

object PathField extends ProductReader[PathField] {
  def apply(): PathField = Impl()

  def apply(mode: Ex[Int]): PathField = {
    val res   = apply()
    res.mode  = mode
    res
  }

  override def read(in: RefMapIn, key: String, arity: Int, adj: Int): PathField = {
    require (arity == 0 && adj == 0)
    PathField()
  }

  val Open  : Ex[Int] = 0
  val Save  : Ex[Int] = 1
  val Folder: Ex[Int] = 2

  private[graph] final val keyValue        = "value"
  private[graph] final val keyTitle        = "title"
  private[graph] final val keyMode         = "mode"
  private[graph] final val defaultValue    = Artifact.Value.empty // new File("")
  private[graph] final val defaultMode     = 0

  object Value extends ProductReader[Value] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Value = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[PathField]()
      new Value(_w)
    }
  }
  final case class Value(w: PathField) extends Ex[Artifact.Value] {
    type Repr[T <: Txn[T]] = IExpr[T, Artifact.Value]

    override def productPrefix: String = s"PathField$$Value" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val ws        = w.expand[T]
      val valueOpt  = ctx.getProperty[Ex[Artifact.Value]](w, keyValue)
      val value0    = valueOpt.fold[Artifact.Value](defaultValue)(_.expand[T].value)
      import ctx.{cursor, targets}
      new PathFieldValueExpandedImpl[T](ws.component, value0).init()  // IntelliJ highlight bug
    }
  }

  private final val titleSeq = List(
    "Open File",
    "Save File",
    "Choose Folder"
  )

  private def defaultTitle(mode: Ex[Int]): Ex[String] = {
    Const(titleSeq).applyOption(mode).getOrElse("Choose")
  }

  object Title extends ProductReader[Title] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Title = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[PathField]()
      new Title(_w)
    }
  }
  final case class Title(w: PathField) extends Ex[String] {
    type Repr[T <: Txn[T]] = IExpr[T, String]

    override def productPrefix: String = s"PathField$$Title" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[String]](w, keyTitle)
      valueOpt.getOrElse(defaultTitle(w.mode)).expand[T]
    }
  }

  object Mode extends ProductReader[Mode] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Mode = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[PathField]()
      new Mode(_w)
    }
  }
  final case class Mode(w: PathField) extends Ex[Int] {
    type Repr[T <: Txn[T]] = IExpr[T, Int]

    override def productPrefix: String = s"PathField$$Mode" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[Int]](w, keyMode)
      valueOpt.getOrElse(Const(defaultMode)).expand[T]
    }
  }

  private final class Expanded[T <: Txn[T]](protected val peer: PathField) extends View[T]
    with ComponentHolder[Peer] with ComponentExpandedImpl[T] {

    type C = Peer

    override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
      val valueOpt  = ctx.getProperty[Ex[Artifact.Value ]](peer, keyValue).map(_.expand[T].value)
      val titleOpt  = ctx.getProperty[Ex[String         ]](peer, keyTitle).map(_.expand[T].value)
      val mode      = ctx.getProperty[Ex[Int            ]](peer, keyMode ).fold(defaultMode)(_.expand[T].value) match {
        case 0 => FileDialog.Open
        case 1 => FileDialog.Save
        case 2 => FileDialog.Folder
        case _ => FileDialog.Open
      }

      deferTx {
        val c = new Peer
        valueOpt.foreach { uri =>
          val fileOpt   = if (!uri.isAbsolute) None else Try(new File(uri)).toOption
          c.valueOption = fileOpt
        }
        titleOpt.foreach(v => c.title = v)
        c.mode    = mode
        component = c
      }
      super.initComponent()
    }
  }

  private final case class Impl() extends PathField with ComponentImpl { w =>
    override def productPrefix: String = "PathField" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] =
      new Expanded[T](this).initComponent()

    object value extends Model[Artifact.Value] {
      def apply(): Ex[Artifact.Value] = Value(w)

      def update(value: Ex[Artifact.Value]): Unit = {
        val b = Graph.builder
        b.putProperty(w, keyValue, value)
      }
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
  type C = Peer

  type Repr[T <: Txn[T]] = View.T[T, C] with IControl[T]

  var title : Ex[String]
  var mode  : Ex[Int]

  def value : Model[Artifact.Value]
}
