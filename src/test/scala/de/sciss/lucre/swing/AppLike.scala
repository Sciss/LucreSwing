package de.sciss.lucre.swing

import javax.swing.UIManager

import de.sciss.desktop.Desktop
import de.sciss.desktop.impl.UndoManagerImpl
import de.sciss.file.File
import de.sciss.lucre.expr
import de.sciss.lucre.stm.Durable
import de.sciss.lucre.stm.store.BerkeleyDB

import scala.swing.{Component, Frame, MainFrame, Menu, MenuBar, MenuItem, SimpleSwingApplication}
import scala.util.control.NonFatal

trait AppLike extends SimpleSwingApplication {
  type S = Durable
  implicit val system = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))

  implicit lazy val undo = new UndoManagerImpl

  override def main(args: Array[String]): Unit = {
    expr.init()

    try {
      val webClassName = "com.alee.laf.WebLookAndFeel"
      UIManager.installLookAndFeel("Web Look And Feel", webClassName)
      UIManager.setLookAndFeel(webClassName)
    } catch {
      case NonFatal(_) =>
        val lafs    = UIManager.getInstalledLookAndFeels
        val gtkOpt  = if (Desktop.isLinux) lafs.find(_.getName contains "GTK+") else None
        gtkOpt.foreach { info =>
        UIManager.setLookAndFeel(info.getClassName)
      }
    }
    super.main(args)
  }

  protected def mkView(): Component

  lazy val top: Frame = {
    val mb = new MenuBar {
      contents += new Menu("Edit") {
        contents += new MenuItem(undo.undoAction)
        contents += new MenuItem(undo.redoAction)
      }
    }

    val res = new MainFrame {
      title = "LucreSwing"
      contents = mkView()
      menuBar = mb
      pack().centerOnScreen()
      open()
    }
    res
  }
}