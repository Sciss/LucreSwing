package de.sciss.lucre.swing

import de.sciss.lucre.expr.Ex
import de.sciss.lucre.expr.impl.ContextMixin
import de.sciss.lucre.stm.{Base, Sys}

object Widget {
  object Builder {
    def apply[S <: Sys[S]](g: Graph): Builder[S] = new Impl[S]

    private final class Impl[S <: Sys[S]] extends ContextMixin[S] with Builder[S]
  }
  trait Builder[S <: Base[S]] extends Ex.Context[S] {

  }

//  trait Expander[+U] extends Widget {
//    // this acts now as a fast unique reference
//    @transient final private[this] lazy val ref = new AnyRef
//
//    // ---- constructor ----
//    Graph.builder.addLazy(this)
//
//    /** A final implementation of this method which calls `visit` on the builder,
//      * checking if this element has already been visited, and if not, will invoke
//      * the `expand` method. Therefore it is guaranteed, that the expansion to
//      * ugens is performed no more than once in the graph expansion.
//      */
//    final private[fscape] def force(b: UGenGraph.Builder): Unit = expand(b)
//
//    /** A final implementation of this method which looks up the current ugen graph
//      * builder and then performs the expansion just as `force`, returning the
//      * expanded object
//      *
//      * @return  the expanded object (e.g. `Unit` for a ugen with no outputs,
//      *          or a single ugen, or a group of ugens)
//      */
//    final private[fscape] def expand(implicit b: UGenGraph.Builder): U = b.visit(ref, makeUGens)
//
//    /** Abstract method which must be implemented by creating the actual `UGen`s
//      * during expansion. This method is at most called once during graph
//      * expansion
//      *
//      * @return  the expanded object (depending on the type parameter `U`)
//      */
//    protected def makeUGens(implicit b: UGenGraph.Builder): U
//  }
}
trait Widget extends Product {
  // this acts now as a fast unique reference
  @transient final private[this] lazy val ref = new AnyRef

  // ---- constructor ----
  Graph.builder.addWidget(this)

  final def expand[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View[S] =
    b.visit[View[S]](ref, mkView)

  protected def mkView[S <: Sys[S]](implicit b: Widget.Builder[S], tx: S#Tx): View[S]
}