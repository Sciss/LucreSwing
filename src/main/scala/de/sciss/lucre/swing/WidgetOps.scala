/*
 *  WidgetOps.scala
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

package de.sciss.lucre.swing

import de.sciss.lucre.expr.{Ex, ExAttr}

import scala.language.implicitConversions

object WidgetOps {
  implicit def exWidgetOps[A](x: Ex[A]): ExWidgetOps[A] = new ExWidgetOps(x)
}
final class ExWidgetOps[A](private val x: Ex[A]) extends AnyVal {
  def ---> (attr: ExAttr.Like[A]): Unit = ExAttrUpdate(x, attr.key)(attr.bridge)
}