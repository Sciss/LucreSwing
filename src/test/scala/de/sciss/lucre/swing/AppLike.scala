package de.sciss.lucre.swing

import de.sciss.lucre.event.Durable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.file.File
import de.sciss.desktop.impl.UndoManagerImpl
import scala.swing.{Component, Swing, GridPanel, MainFrame, MenuItem, Menu, MenuBar, Frame, SimpleSwingApplication}
import de.sciss.desktop.Desktop
import javax.swing.UIManager
import scala.util.control.NonFatal

trait AppLike extends SimpleSwingApplication {
  type S = Durable
  implicit val system = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))

  implicit lazy val undo = new UndoManagerImpl {
    protected var dirty: Boolean = false
  }

  override def main(args: Array[String]): Unit = {
    val lafs    = UIManager.getInstalledLookAndFeels
    val gtkOpt  = if (Desktop.isLinux) lafs.find(_.getName contains "GTK+") else None
    try {
      val webClassName = "com.alee.laf.WebLookAndFeel"
      UIManager.installLookAndFeel("Web Look And Feel", webClassName)
      UIManager.setLookAndFeel(webClassName)
    } catch {
      case NonFatal(_) => gtkOpt.foreach { info =>
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