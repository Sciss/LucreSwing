package de.sciss.lucre.swing

import scala.swing.{GridPanel, MainFrame, Frame, SimpleSwingApplication}
import de.sciss.lucre.event.Durable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.file.File
import scala.collection.immutable.{IndexedSeq => Vec}
import de.sciss.lucre.expr
import de.sciss.desktop.impl.UndoManagerImpl

object TestApp extends SimpleSwingApplication {
  type S = Durable
  private implicit val system = Durable(BerkeleyDB.factory(File.createTemp(directory = true)))

  private val rows = 2

  private implicit val undo = new UndoManagerImpl {
    protected var dirty: Boolean = false
  }

  private val views: Vec[View[S]] = system.step { implicit tx =>
    val exprD1  = expr.Double.newVar[S](expr.Double.newConst(0.0))
    val exprI1  = expr.Int   .newVar[S](expr.Int   .newConst(0  ))
    val vD1     = DoubleSpinnerView(exprD1, "d1")
    val vD2     = DoubleSpinnerView(exprD1, "d2")
    //    val vI1     = IntSpinnerView   (exprI1, "i1")
    //    val vI2     = IntSpinnerView   (exprI1, "i2")

    Vec(
      vD1, vD2 // , vI1, vI2
    )
  }

  lazy val top: Frame = {
    new MainFrame {
      title = "LucreSwing"
      contents = new GridPanel(rows0 = rows, cols0 = views.size/rows) {
        contents ++= views.map(_.component)
      }
      pack().centerOnScreen()
      open()
    }
  }
}
