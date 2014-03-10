package de.sciss.lucre.swing

import scala.swing.{MenuItem, Menu, MenuBar, Swing, Alignment, Label, GridPanel, MainFrame, Frame, SimpleSwingApplication}
import de.sciss.lucre.event.Durable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.file.File
import scala.collection.immutable.{IndexedSeq => Vec}
import de.sciss.lucre.expr
import de.sciss.desktop.impl.UndoManagerImpl
import de.sciss.desktop.Desktop
import javax.swing.{SwingUtilities, UIManager}

object TestApp extends SimpleSwingApplication {
  type S = Durable
  private implicit val system = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))

  private val rows = 3

  private implicit val undo = new UndoManagerImpl {
    protected var dirty: Boolean = false
  }

  private val views: Vec[View[S]] = system.step { implicit tx =>
    val exprD1  = expr.Double.newVar[S](expr.Double.newConst(0.0  ))
    val exprI1  = expr.Int   .newVar[S](expr.Int   .newConst(0    ))
    val exprS1  = expr.String.newVar[S](expr.String.newConst("Foo"))
    val vD1     = DoubleSpinnerView(exprD1, "d1")
    val vD2     = DoubleSpinnerView(exprD1, "d2")
    val vI1     = IntSpinnerView   (exprI1, "i1")
    val vI2     = IntSpinnerView   (exprI1, "i2")
    val vS1     = StringFieldView  (exprS1, "s1")
    val vS2     = StringFieldView  (exprS1, "s2")

    def label(text: String) = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))

    Vec(
      label("Double"), vD1, vD2, label("Int"), vI1, vI2, label("String"), vS1, vS2
    )
  }

  override def main(args: Array[String]): Unit = {
    if (Desktop.isLinux) UIManager.getInstalledLookAndFeels.find(_.getName contains "GTK+").foreach { info =>
      UIManager.setLookAndFeel(info.getClassName)
    }
    super.main(args)
  }

  lazy val top: Frame = {
    val mb = new MenuBar {
      contents += new Menu("Edit") {
        contents += new MenuItem(undo.undoAction)
        contents += new MenuItem(undo.redoAction)
      }
    }

    val res = new MainFrame {
      title = "LucreSwing"
      contents = new GridPanel(rows0 = rows, cols0 = views.size/rows) {
        vGap = 2
        hGap = 2
        border = Swing.EmptyBorder(4)
        contents ++= views.map(_.component)
      }
      menuBar = mb
      pack().centerOnScreen()
      open()
    }
    // SwingUtilities.updateComponentTreeUI(res.peer)
    // SwingUtilities.updateComponentTreeUI(res.peer)
    res
  }
}
