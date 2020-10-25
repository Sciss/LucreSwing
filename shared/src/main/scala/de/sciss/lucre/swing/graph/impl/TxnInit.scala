package de.sciss.lucre.swing.graph.impl

import de.sciss.lucre.Txn

trait TxnInit[T <: Txn[T]] {
  def init()(implicit tx: T): this.type
}
