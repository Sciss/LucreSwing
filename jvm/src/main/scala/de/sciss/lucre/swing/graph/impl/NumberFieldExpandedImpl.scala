///*
// *  IntFieldExpandedImpl.scala
// *  (LucreSwing)
// *
// *  Copyright (c) 2014-2020 Hanns Holger Rutz. All rights reserved.
// *
// *	This software is published under the GNU Affero General Public License v3+
// *
// *
// *	For further information, please contact Hanns Holger Rutz at
// *	contact@sciss.de
// */
//
//package de.sciss.lucre.swing
//package graph
//package impl
//
//import java.text.{NumberFormat, ParseException}
//import java.util.Locale
//
//import de.sciss.audiowidgets.{ParamFormat, UnitView}
//import de.sciss.audiowidgets.{ParamField => Peer}
//import de.sciss.lucre.{Cursor, IExpr, ITargets, Txn}
//import de.sciss.lucre.expr.{Context, IControl}
//import de.sciss.lucre.expr.graph.Ex
//import de.sciss.lucre.swing.LucreSwing.deferTx
//import de.sciss.lucre.swing.graph.IntField.{defaultEditable, defaultMax, defaultMin, defaultPrototype, defaultStep, defaultUnit, defaultValue, keyEditable, keyMax, keyMin, keyPrototype, keyStep, keyUnit, keyValue}
//import de.sciss.lucre.swing.impl.ComponentHolder
//import de.sciss.numbers
//import javax.swing.text.NumberFormatter
//
//import scala.collection.immutable.{Seq => ISeq}
//import scala.util.Try
//
//abstract class NumberFieldExpandedImpl[T <: Txn[T], A, F <: NumberField[A]](protected val peer: F, tx0: T)(implicit ctx: Context[T])
//  extends View[T]
//    with ComponentHolder[View.IntField] with ComponentExpandedImpl[T] with IControl[T] {
//
//  protected def _value: IExpr[T, A] with TxnInit[T]
//
//  protected def immutable[B](in: Seq[B]): ISeq[B] =
//    in match {
//      case ix: ISeq[B]  => ix
//      case _            => in.toList
//    }
//
//  override def initComponent()(implicit tx: T, ctx: Context[T]): this.type = {
//    super.initComponent()
//    _value.init()
//    this
//  }
//
//  override def dispose()(implicit tx: T): Unit = {
//    super.dispose()
//    _value.dispose()
//  }
//}