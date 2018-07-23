package de.sciss.lucre.swing

import de.sciss.audiowidgets.DualRangeModel
import de.sciss.lucre.expr.{BooleanObj, DoubleObj, IntObj, StringObj}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{Alignment, Component, GridPanel, Label, Swing}

object TestExprApp extends AppLike {
  // de.sciss.lucre.event.showLog = true

  private val rows = 5

  private lazy val views: Vec[View[S]] = system.step { implicit tx =>
//    implicit val doubleEx  = DoubleObj
//    implicit val booleanEx = BooleanObj

    val exprD1  = DoubleObj .newVar[S](DoubleObj .newConst( 0.0 ))
    val exprI1  = IntObj    .newVar[S](IntObj    .newConst( 0   ))
    val exprI2  = IntObj    .newVar[S](IntObj    .newConst(10   ))
    val exprS1  = StringObj .newVar[S](StringObj .newConst("Foo"))
    val exprB1  = BooleanObj.newVar[S](BooleanObj.newConst(true ))
    val vD1     = DoubleSpinnerView  (exprD1, "d1")
    val vD2     = DoubleSpinnerView  (exprD1, "d2")
    val vI1     = IntSpinnerView     (exprI1, "i1")
    val vI2     = IntSpinnerView     (exprI2, "i2")
    val vS1     = StringFieldView    (exprS1, "s1")
    val vS2     = StringFieldView    (exprS1, "s2")
    val vB1     = BooleanCheckBoxView(exprB1, "b1")
    val vB2     = BooleanCheckBoxView(exprB1, "b2")   // mirrors b1

    val vI3     = IntRangeSliderView(DualRangeModel(), "i3")
    vI3.value   = Some(exprI1)
    val vI4     = IntRangeSliderView(DualRangeModel(), "i3...i4")
    vI4.rangeLo = Some(exprI1)
    vI4.rangeHi = Some(exprI2)

    def label(text: String): View.T[S, Label] = View.wrap(new Label(s"$text:", null, Alignment.Trailing))

    Vec(
      label("Double"), vD1, vD2,
      label("Int"   ), vI1, vI2,
      label("String"), vS1, vS2,
      label("Int(s)"), vI3, vI4,
      label("Boolean"), vB1, vB2
    )
  }

  def mkView(): Component = new GridPanel(rows0 = rows, cols0 = views.size/rows) {
    vGap = 2
    hGap = 2
    border = Swing.EmptyBorder(4)
    contents ++= views.map(_.component)
  }
}
