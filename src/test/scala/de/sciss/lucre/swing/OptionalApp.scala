package de.sciss.lucre.swing

import de.sciss.lucre.expr
import de.sciss.lucre.expr.Expr
import de.sciss.model.Change

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.swing.{Button, Alignment, Component, GridPanel, Label, Swing}

object OptionalApp extends AppLike {
  // de.sciss.lucre.event.showLog = true

  private val rows = 1

  private lazy val views: Vec[View[S]] = system.step { implicit tx =>
    implicit val doubleEx  = de.sciss.lucre.expr.Double
    implicit val booleanEx = de.sciss.lucre.expr.Boolean

    import doubleEx.{serializer => doubleSer, varSerializer => doubleVarSer}

    val exprD1  = doubleEx.newVar[S](doubleEx.newConst(0.0))
    val map     = expr.Map.Modifiable[S, String, Expr[S, Double], Change[Double]]
    val key     = "foo"
    val mapView = CellView.exprMap[S, String, Double](map, key)
    val vD1     = DoubleSpinnerView(exprD1, "d1")
    val vD2     = DoubleSpinnerView.optional[S](mapView, "d2")

    def label(text: String) = View.wrap[S](new Label(s"$text:", null, Alignment.Trailing))

    def button(text: String)(action: => Unit) = View.wrap[S](Button(text)(action))

    val mapH    = tx.newHandle(map)
    val exprD1H = tx.newHandle(exprD1)

    val butPut    = button("Put"   )(system.step { implicit tx => mapH().put   (key, exprD1H()) })
    val butRemove = button("Remove")(system.step { implicit tx => mapH().remove(key           ) })

    Vec(
      label("Double"), vD1, vD2, butPut, butRemove
    )
  }

  def mkView(): Component = new GridPanel(rows0 = rows, cols0 = views.size/rows) {
    vGap = 2
    hGap = 2
    border = Swing.EmptyBorder(4)
    contents ++= views.map(_.component)
  }
}
