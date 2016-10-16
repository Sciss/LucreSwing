package de.sciss.lucre.swing

import de.sciss.lucre.expr.{DoubleObj, IntObj}
import de.sciss.lucre.{event => evt}
import de.sciss.swingplus.GroupPanel

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.event.ButtonClicked
import scala.swing.{Alignment, Button, Component, Label, ToggleButton}

object OptionalApp extends AppLike {
  // de.sciss.lucre.event.showLog = true

  // private val rows = 1

  private lazy val viewsDouble: Vec[View[S]] = system.step { implicit tx =>
    implicit val doubleEx   = DoubleObj
    implicit val intEx      = IntObj

    def label (text: String)                  = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))
    def button(text: String)(action: => Unit) = View.wrap[S](Button(text)(action))

    val exDouble1       = DoubleObj.newVar[S](DoubleObj.newConst(0.0))
    val mapDouble       = evt.Map.Modifiable[S, String, DoubleObj]
    val keyDouble       = "foo-Double"
    val mapViewDouble   = CellView.exprMap[S, String, Double, DoubleObj](mapDouble, keyDouble)
    val vDouble1        = DoubleSpinnerView(exDouble1, "d1")
    val vDouble2        = DoubleSpinnerView.optional[S](mapViewDouble, "d2")
  
    val mapDoubleH      = tx.newHandle(mapDouble)
    val exDouble1H      = tx.newHandle(exDouble1)

    val butPutDouble    = button("Put"   )(system.step { implicit tx => mapDoubleH().put   (keyDouble, exDouble1H()) })
    val butRemoveDouble = button("Remove")(system.step { implicit tx => mapDoubleH().remove(keyDouble           ) })

    val togDefaultDouble = new ToggleButton("Default: 0") {
      listenTo(this)
      reactions += {
        case ButtonClicked(_) => vDouble2.default = if (selected) Some(0.0) else None
      }
    }

    Vec(
      label("Double"), vDouble1, vDouble2, butPutDouble, butRemoveDouble, View.wrap[S](togDefaultDouble)
    )
  }

  private lazy val viewsInt: Vec[View[S]] = system.step { implicit tx =>
    implicit val doubleEx   = DoubleObj
    implicit val intEx      = IntObj

    def label (text: String)                  = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))
    def button(text: String)(action: => Unit) = View.wrap[S](Button(text)(action))

    val exInt1       = IntObj.newVar[S](IntObj.newConst(0))
    val mapInt       = evt.Map.Modifiable[S, String, IntObj]
    val keyInt       = "foo-Int"
    val mapViewInt   = CellView.exprMap[S, String, Int, IntObj](mapInt, keyInt)
    val vInt1        = IntSpinnerView(exInt1, "d1")
    val vInt2        = IntSpinnerView.optional[S](mapViewInt, "d2")

    val mapIntH      = tx.newHandle(mapInt)
    val exInt1H      = tx.newHandle(exInt1)

    val butPutInt    = button("Put"   )(system.step { implicit tx => mapIntH().put   (keyInt, exInt1H()) })
    val butRemoveInt = button("Remove")(system.step { implicit tx => mapIntH().remove(keyInt           ) })

    val togDefaultInt = new ToggleButton("Default: 0") {
      listenTo(this)
      reactions += {
        case ButtonClicked(_) => vInt2.default = if (selected) Some(0) else None
      }
    }

    Vec(
      label("Int"), vInt1, vInt2, butPutInt, butRemoveInt, View.wrap[S](togDefaultInt)
    )
  }

  def mkView(): Component =
    new GroupPanel {
      horizontal = Seq((viewsDouble zip viewsInt).map { case (v1, v2) =>
        Par(v1.component, v2.component)
      } : _* )
      vertical = Seq(
        Par(Baseline)(viewsDouble.map(v => GroupPanel.Element(v.component)): _*), // can't use implicit conversion in Scala 2.10
        Par(Baseline)(viewsInt   .map(v => GroupPanel.Element(v.component)): _*))
    }
}
