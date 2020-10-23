/*
 *  View.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing

import com.raquo.laminar.nodes.ReactiveHtmlElement
import de.sciss.lucre.{Disposable, Txn}

object View {
  type T[Tx <: Txn[Tx], C1 <: Component] = View[Tx] { type C = C1 }

  type Component = ReactiveHtmlElement.Base
}
trait View[T <: Txn[T]] extends Disposable[T] {
  type C <: View.Component

  def component: C
}
