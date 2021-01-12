/*
 *  Border.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2021 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Affero General Public License v3+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import de.sciss.lucre.expr.graph.Ex

object Border {
  object Empty {
    def apply(weight: Int): Border = Empty(weight, weight, weight, weight)

    def apply(top: Int, left: Int, bottom: Int, right: Int): Border =
      EmptyImpl(top = top, left = left, bottom = bottom, right = right)

//    def unapply(b: Empty): Option[Empty] = Some(b)
  }
  trait Empty extends Border {
    def top   : Int
    def left  : Int
    def bottom: Int
    def right : Int
  }

  private final case class EmptyImpl(top: Int, left: Int, bottom: Int, right: Int)
    extends Empty {

    override def productPrefix: String = s"Border$$Empty" // serialization

//    private[swing] def mkPeer(): Peer = {
//      // if (top == 0 && left == 0 && bottom == 0 && right == 0)
//      Swing.EmptyBorder(top = top, left = left, bottom = bottom, right = right)
//    }
  }

  implicit object ExValue extends Ex.Value[Border]
}
sealed trait Border extends Product {
  // private[swing] def mkPeer(): Peer
}
