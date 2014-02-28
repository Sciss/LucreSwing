/*
 *  ComponentHolder.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU General Public License v2+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre
package swing
package impl

trait ComponentHolder[C] {
  private var comp = Option.empty[C]

  final protected def component_=(c: C): Unit = {
    requireEDT()
    if(comp.nonEmpty) throw new IllegalStateException("Component has already been set")
    comp = Some(c)
  }

  final def component: C = {
    requireEDT()
    comp.getOrElse(throw new IllegalStateException("Called component before GUI was initialized"))
  }
}