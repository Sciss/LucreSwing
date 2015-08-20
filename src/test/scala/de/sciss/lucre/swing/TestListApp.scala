package de.sciss.lucre.swing

import de.sciss.lucre
import de.sciss.lucre.expr
import de.sciss.lucre.expr.Expr
import de.sciss.model.Change

import scala.concurrent.stm.{Ref, Txn}
import scala.swing.{BorderPanel, Button, Component, FlowPanel}

object TestListApp extends AppLike {
  implicit private val listModSer = expr.List.Modifiable.serializer[S, Expr[S, String]]
  implicit private val listSer    = expr.List.serializer[S, Expr[S, String]]

  private val h     = ListView.Handler[S, Expr[S, String], Change[String]] {
    implicit tx => _.value
  } {
    implicit tx => (_, ch) => Some(ch.now)
  }

  private lazy val (listH, view) = system.step { implicit tx =>
    val li = expr.List.Modifiable[S, Expr[S, String]]
    tx.newHandle(li) -> ListView(li, h)
  }

  private def scramble(s: String): String = {
    val sb  = new StringBuilder
    var bag = (0 until s.length).toIndexedSeq
    while (bag.nonEmpty) {
      val i = (math.random * bag.size).toInt
      val j = bag(i)
      bag   = bag.patch(i, Nil, 1)
      sb.append(s.charAt(j))
    }
    sb.result()
  }

  private def addAction(): Unit = {
    val text    = scramble("Vermisste Boeing umgeleitet")
    val retries = Ref(3)
    system.step { implicit tx =>
      implicit val itx = tx.peer
      val s     = lucre.expr.String.newConst[S](text)
      val list  = listH()
      list.addLast(s)
      if (retries() > 0) {
        new Thread {
          override def run(): Unit = {
            println("retrying...")
            retries.single -= 1
          }
          start()
        }
        Txn.retry
      }
    }
  }

  private def removeAction(): Unit = {
    val sel = view.guiSelection.sorted.reverse
    if (sel.nonEmpty) system.step { implicit tx =>
      val list = listH()
      sel.foreach { idx => list.removeAt(idx) }
    }
  }

  protected def mkView(): Component = new BorderPanel {
    add(view.component, BorderPanel.Position.Center)
    add(new FlowPanel(
      Button("Add"   )(addAction()),
      Button("Remove")(removeAction())
    ), BorderPanel.Position.South)
  }
}
