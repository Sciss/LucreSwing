/*
 *  Panel.scala
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

import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.ExElem.{ProductReader, RefMapIn}
import de.sciss.lucre.expr.graph.{Const, Ex}
import de.sciss.lucre.swing.graph.{Border => _Border}
import de.sciss.lucre.{IExpr, Txn}

object Panel {
  object Border extends ProductReader[Border] {
    override def read(in: RefMapIn, key: String, arity: Int, adj: Int): Border = {
      require (arity == 1 && adj == 0)
      val _w = in.readProductT[Panel]()
      new Border(_w)
    }
  }
  final case class Border(w: Panel) extends Ex[_Border] {
    type Repr[T <: Txn[T]] = IExpr[T, _Border]

    override def productPrefix: String = s"Panel$$Border" // serialization

    protected def mkRepr[T <: Txn[T]](implicit ctx: Context[T], tx: T): Repr[T] = {
      val valueOpt = ctx.getProperty[Ex[_Border]](w, keyBorder)
      valueOpt.fold(Const(defaultBorder).expand[T])(_.expand[T])
    }
  }

  private[graph] final val keyBorder              = "border"
  private[graph] final val defaultBorder: _Border = _Border.Empty(0)
}
trait Panel extends Component {
  def contents: Seq[Widget]

  var border: Ex[Border]
}
