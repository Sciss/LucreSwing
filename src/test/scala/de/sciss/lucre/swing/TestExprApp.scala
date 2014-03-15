package de.sciss.lucre.swing

import scala.swing.{Component, MenuItem, Menu, MenuBar, Swing, Alignment, Label, GridPanel, MainFrame, Frame, SimpleSwingApplication}
import de.sciss.lucre.event.Durable
import de.sciss.lucre.stm.store.BerkeleyDB
import de.sciss.file.File
import scala.collection.immutable.{IndexedSeq => Vec}
import de.sciss.lucre.expr
import de.sciss.desktop.impl.UndoManagerImpl
import de.sciss.desktop.Desktop
import javax.swing.{SwingUtilities, UIManager}
import de.sciss.audiowidgets.DualRangeModel

object TestExprApp extends AppLike {
  // de.sciss.lucre.event.showLog = true

  private val rows = 4

  private val views: Vec[View[S]] = system.step { implicit tx =>
    val exprD1  = expr.Double.newVar[S](expr.Double.newConst( 0.0 ))
    val exprI1  = expr.Int   .newVar[S](expr.Int   .newConst( 0   ))
    val exprI2  = expr.Int   .newVar[S](expr.Int   .newConst(10   ))
    val exprS1  = expr.String.newVar[S](expr.String.newConst("Foo"))
    val vD1     = DoubleSpinnerView(exprD1, "d1")
    val vD2     = DoubleSpinnerView(exprD1, "d2")
    val vI1     = IntSpinnerView   (exprI1, "i1")
    val vI2     = IntSpinnerView   (exprI2, "i2")
    val vS1     = StringFieldView  (exprS1, "s1")
    val vS2     = StringFieldView  (exprS1, "s2")

    val vI3     = IntRangeSliderView(DualRangeModel(), "i3")
    vI3.value   = Some(exprI1)
    val vI4     = IntRangeSliderView(DualRangeModel(), "i3...i4")
    vI4.rangeLo = Some(exprI1)
    vI4.rangeHi = Some(exprI2)

    def label(text: String) = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))

    Vec(
      label("Double"), vD1, vD2,
      label("Int"   ), vI1, vI2,
      label("String"), vS1, vS2,
      label("Int(s)"), vI3, vI4
    )
  }

  def mkView(): Component = new GridPanel(rows0 = rows, cols0 = views.size/rows) {
    vGap = 2
    hGap = 2
    border = Swing.EmptyBorder(4)
    contents ++= views.map(_.component)
  }
}
