/*
 *  Panel.scala
 *  (LucreSwing)
 *
 *  Copyright (c) 2014-2019 Hanns Holger Rutz. All rights reserved.
 *
 *	This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *	For further information, please contact Hanns Holger Rutz at
 *	contact@sciss.de
 */

package de.sciss.lucre.swing
package graph

import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.expr.{Context, IExpr}
import de.sciss.lucre.stm.Sys

object Panel {
  final case class Border(w: Panel) extends Ex[graph.Border] {
    type Repr[S <: Sys[S]] = IExpr[S, graph.Border]

    override def productPrefix: String = s"Panel$$Border" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val valueOpt = ctx.getProperty[Ex[graph.Border]](w, keyBorder)
      valueOpt.fold(Const(defaultBorder).expand[S])(_.expand[S])
    }
  }

  private[graph] final val keyBorder                    = "border"
  private[graph] final val defaultBorder: graph.Border  = graph.Border.Empty(0)
}
trait Panel extends Component {
  def contents: Seq[Widget]

  var border: Ex[Border]
}
