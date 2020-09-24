package de.sciss.lucre.swing

import de.sciss.lucre.{ListObj, StringObj}
import de.sciss.model.Change
import de.sciss.serial.TFormat

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.concurrent.stm.{InTxn, Ref, Txn}
import scala.swing.{BorderPanel, Button, Component, FlowPanel}

object TestListApp extends DurableAppLike {
  implicit val listModSer: TFormat[T, ListObj.Modifiable[T, StringObj[T]]] = ListObj.Modifiable.format[T, StringObj[T]]
  implicit val listSer   : TFormat[T, ListObj           [T, StringObj[T]]] = ListObj           .format[T, StringObj[T]]

  private val h     = ListView.Handler[T, StringObj[T], Change[String]] {
    implicit tx => _.value
  } {
    _ => (_, ch) => Some(ch.now)
  }

  private lazy val (listH, view) = system.step { implicit tx =>
    val li = ListObj.Modifiable[T, StringObj]
    tx.newHandle(li) -> ListView(li, h)
  }

  private def scramble(s: String): String = {
    val sb  = new StringBuilder
    var bag: Vec[Int] = 0 until s.length
    while (bag.nonEmpty) {
      val i = (math.random() * bag.size).toInt
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
      implicit val itx: InTxn = tx.peer
      val s     = StringObj.newConst[T](text)
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
