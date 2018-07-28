package de.sciss.lucre.swing

import de.sciss.lucre.aux.ProductWithAux
import de.sciss.lucre.stm.{Disposable, Sys}

trait Control extends ProductWithAux {
  // this acts now as a fast unique reference
  @transient final private[this] lazy val ref = new AnyRef

  // ---- constructor ----
  Graph.builder.addControl(this)

  final def expand[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): Disposable[S#Tx] =
    b.visit[Disposable[S#Tx]](ref, mkControl)

  protected def mkControl[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): Disposable[S#Tx]
}
