/*
 *  ComboBoxExpandedImpl.scala
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

package de.sciss.lucre.swing
package graph
package impl

import de.sciss.lucre.expr.Context
import de.sciss.lucre.expr.graph.{Ex, UnaryOp}
import de.sciss.lucre.swing.graph.ComboBox.{keyIndex, keyValueOption}
import de.sciss.lucre.{IExpr, Txn}

abstract class ComboBoxExpandedImpl[T <: Txn[T], A](protected val peer: ComboBox[A], tx0: T)(implicit ctx: Context[T])
  extends ComponentExpandedImpl[T]
    with ComboBox.Repr[T, A] {

  protected def mkValueExpanded(init: (Int, Option[A])): IExpr[T, (Int, Option[A])] with TxnInit[T]

    // it is important to create a single source of transactions
  // for index and valueOption, otherwise there can be glitches
  // where gui and tx state diverge for one of them
  protected val _value: IExpr[T, (Int, Option[A])] with TxnInit[T] = {
    implicit val tx: T = tx0

    val indexOpt  = ctx.getProperty[Ex[Int]](peer, keyIndex)
    val index0    = indexOpt.fold[Int]({
      val vec = peer.items.expand[T].value
      if (vec.isEmpty) -1 else 0
    })(_.expand[T].value)

    val itemOpt   = ctx.getProperty[Ex[Option[A]]](peer, keyValueOption)
    val item0     = itemOpt.fold[Option[A]]({
      val vec = peer.items.expand[T].value
      vec.headOption
    })(_.expand[T].value)

    mkValueExpanded((index0, item0))
  }

  private[graph] val index: IExpr[T, Int] =
    new UnaryOp.Expanded[T, (Int, Option[A]), Int](UnaryOp.Tuple2_1(), _value, tx0)(ctx.targets)

  private[graph] val valueOption: IExpr[T, Option[A]] =
    new UnaryOp.Expanded[T, (Int, Option[A]), Option[A]](UnaryOp.Tuple2_2(), _value, tx0)(ctx.targets)

  override def dispose()(implicit tx: T): Unit = {
    super.dispose()

    _value     .dispose()
    index      .dispose()
    valueOption.dispose()
  }
}
