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

import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.Ex

object Border {
  object Empty extends ProductReader[Empty] {
    def apply(weight: Int): Empty = Empty(weight, weight, weight, weight)

    def apply(top: Int, left: Int, bottom: Int, right: Int): Empty =
      EmptyImpl(top = top, left = left, bottom = bottom, right = right)

    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Empty = {
      require (arity == 4 && adj == 0)
      val _top    = in.readInt()
      val _left   = in.readInt()
      val _bottom = in.readInt()
      val _right  = in.readInt()
      Empty(_top, _left, _bottom, _right)
    }
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
