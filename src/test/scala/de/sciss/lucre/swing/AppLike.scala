package de.sciss.lucre.swing

import de.sciss.lucre.event.Durable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.file.File
import de.sciss.desktop.impl.UndoManagerImpl
import scala.swing.{Component, Swing, GridPanel, MainFrame, MenuItem, Menu, MenuBar, Frame, SimpleSwingApplication}
import de.sciss.desktop.Desktop
import javax.swing.UIManager

trait AppLike extends SimpleSwingApplication {
  type S = Durable
  implicit val system = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))

  implicit val undo = new UndoManagerImpl {
    protected var dirty: Boolean = false
  }

  override def main(args: Array[String]): Unit = {
    if (Desktop.isLinux) UIManager.getInstalledLookAndFeels.find(_.getName contains "GTK+").foreach { info =>
      UIManager.setLookAndFeel(info.getClassName)
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