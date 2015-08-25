package de.sciss.lucre.swing

import de.sciss.lucre.{event => evt}
import de.sciss.lucre.expr.{BooleanObj, DoubleObj, StringObj}

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.event.ButtonClicked
import scala.swing.{Alignment, Button, Component, FlowPanel, Label, ToggleButton}

object OptionalApp extends AppLike {
  // de.sciss.lucre.event.showLog = true

  // private val rows = 1

  private lazy val views: Vec[View[S]] = system.step { implicit tx =>
    implicit val doubleEx  = DoubleObj
    implicit val booleanEx = BooleanObj
    implicit val stringEx  = StringObj

    val exprD1  = doubleEx.newVar[S](doubleEx.newConst(0.0))
    val map     = evt.Map.Modifiable[S, String, DoubleObj]
    val key     = "foo"
    val mapView = CellView.exprMap[S, String, Double, DoubleObj](map, key)
    val vD1     = DoubleSpinnerView(exprD1, "d1")
    val vD2     = DoubleSpinnerView.optional[S](mapView, "d2")
    // vD2.default = Some(1234.0)

    def label(text: String) = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))

    def button(text: String)(action: => Unit) = View.wrap[S](Button(text)(action))

    val mapH    = tx.newHandle(map)
    val exprD1H = tx.newHandle(exprD1)

    val butPut    = button("Put"   )(system.step { implicit tx => mapH().put   (key, exprD1H()) })
    val butRemove = button("Remove")(system.step { implicit tx => mapH().remove(key           ) })

    val togDefault = new ToggleButton("Default: 0") {
      listenTo(this)
      reactions += {
        case ButtonClicked(_) => vD2.default = if (selected) Some(0.0) else None
      }
    }

    Vec(
      label("Double"), vD1, vD2, butPut, butRemove, View.wrap[S](togDefault)
    )
  }

  def mkView(): Component = new FlowPanel(views.map(_.component): _*)
}
