package de.sciss.lucre.swing

import de.sciss.lucre.expr.{ExOps, IntObj, StringObj}
import de.sciss.lucre.stm.InMemory

import scala.swing.Component

object ExprAttrTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
      val attr1 = "dice".attr[Int]
      val attr2 = "dice".attr[Int](-1)
      FlowPanel(Label(attr1.toStr), Label(attr2.toStr))
    }

    type              S = InMemory
    implicit val sys: S = InMemory()

    val (view, selfH) = sys.step { implicit tx =>
      val self = StringObj.newConst[S]("foo"): StringObj[S]
      val _view = g.expand[S](Some(self))
      _view -> tx.newHandle(self)
    }

    new scala.swing.FlowPanel(
      view.component,
      scala.swing.Button("Dice") {
        val i = (math.random() * 6).toInt + 1
        sys.step { implicit tx =>
          val value = IntObj.newConst[S](i)
          val attr  = selfH().attr
          attr.put("dice", value)
        }
      },
      scala.swing.Button("Clear") {
        sys.step { implicit tx =>
          val attr  = selfH().attr
          attr.remove("dice")
        }
      }
    )
  }
}