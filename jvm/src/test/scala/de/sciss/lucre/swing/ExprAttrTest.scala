package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Context, ExImport}
import de.sciss.lucre.{IntObj, StringObj}

import scala.swing.Component

object ExprAttrTest extends InMemoryAppLike {
  protected def mkView(): Component = {
    import ExImport._
    import graph._

    val keyDice   = "dice"
    val keySlider = "slider"

    val g = Graph {
      val attr1 = keyDice.attr[Int]
      val attr2 = keyDice.attr[Int](-1)
      val sl    = Slider()
      sl.value <--> keySlider.attr(0)
      FlowPanel(Label(attr1.toStr), Label(attr2.toStr), sl)
    }

    val (view, selfH) = system.step { implicit tx =>
      val self  = StringObj.newConst[T]("foo"): StringObj[T]
      val selfH = tx.newHandle(self)
      implicit val ctx: Context[T] = Context(Some(selfH))
      val _view = g.expand[T]
      _view -> selfH
    }

    new scala.swing.FlowPanel(
      view.component,
      scala.swing.Button("Dice") {
        val i = (math.random() * 6).toInt + 1
        system.step { implicit tx =>
          val value = IntObj.newConst[T](i)
          val attr  = selfH().attr
          attr.put(keyDice, value)
        }
      },
      scala.swing.Button("Clear") {
        system.step { implicit tx =>
          val attr = selfH().attr
          attr.remove(keyDice)
        }
      }
    )
  }
}