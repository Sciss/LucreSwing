package de.sciss.lucre.swing
package graph
package impl

import com.raquo.laminar.api.L._
import de.sciss.lucre.{Cursor, ITargets, Txn}
import org.scalajs.dom

final class TextFieldTextExpandedImpl[T <: Txn[T]](peer: => View.TextField, value0: String)
                                                  (implicit targets: ITargets[T], cursor: Cursor[T])
  extends ComponentPropertyExpandedImpl[T, String](value0) {

  protected def valueOnEDT: String = peer.ref.value

  protected def startListening(): Unit = {
    val c = peer
    c.amend(
      onChange --> Observer[dom.Event](_ => commit()),
    )
  }

  protected def stopListening(): Unit = ()
}
