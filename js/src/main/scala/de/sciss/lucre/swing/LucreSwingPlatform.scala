package de.sciss.lucre.swing

import scala.concurrent.stm.Txn

trait LucreSwingPlatform {
  protected def initPlatform(): Unit = ()

  def requireEDT(): Unit = ()

  def defer(thunk: => Unit): Unit = {
    if (Txn.findCurrent.isDefined) throw new IllegalStateException("Trying to execute GUI code inside a transaction")
    thunk
  }
}
