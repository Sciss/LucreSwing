/*
 *  Border.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2018 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing.graph

import javax.swing.border.{Border => Peer}

import scala.swing.Swing

object Border {
  object Empty {
    def apply(weight: Int): Border = Empty(weight, weight, weight, weight)

    def apply(top: Int, left: Int, bottom: Int, right: Int): Border =
      EmptyImpl(top = top, left = left, bottom = bottom, right = right)
  }

  private final case class EmptyImpl(top: Int, left: Int, bottom: Int, right: Int)
    extends Border {

    override def productPrefix: String = s"Border$$Empty" // serialization

    private[swing] def mkPeer(): Peer = {
      // if (top == 0 && left == 0 && bottom == 0 && right == 0)
      Swing.EmptyBorder(top = top, left = left, bottom = bottom, right = right)
    }
  }
}
trait Border extends Product {
  private[swing] def mkPeer(): Peer
}
