/*
 *  IntRangeSliderView.scala
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

import de.sciss.audiowidgets.{DualRangeModel, DualRangeSlider}
import de.sciss.desktop.UndoManager
import de.sciss.lucre.{Cursor, IntObj, Txn}
import de.sciss.lucre.swing.impl.{IntRangeSliderViewImpl => Impl}

object IntRangeSliderView {
  /** Creates a new range slider from a given model, with no expressions initially associated.
    * Expressions are associated using subsequent calls to `value_=`, `rangeLo_=`, `rangeHi_=`
    * or `extent_=`.
    *
    * @param  model0    the initial range slider model
    * @param  name      the name is used as a text string in undoable edits
    * @param  width     the preferred visual extent of the slider in pixels
    */
  def apply[T <: Txn[T]](model0: DualRangeModel, name: String, width: Int = 160)
                        (implicit tx: T, cursor: Cursor[T], undoManager: UndoManager): IntRangeSliderView[T] =
    Impl(model0 = model0, name = name, width = width)
}
trait IntRangeSliderView[T <: Txn[T]] extends View[T] {
  type C = DualRangeSlider

  /** Gets the expression associated with the single value slider (if any). */
  def value                                (implicit tx: T): Option[IntObj[T]]

  /** Sets the expression associated with the single value slider. The value `None`
    * can be used to disable the single value slider function (it will be hidden).
    */
  def value_=  (expr: Option[IntObj[T]])(implicit tx: T): Unit

  /** Gets the expression associated with the lower bound of the range slider (if any). */
  def rangeLo                              (implicit tx: T): Option[IntObj[T]]

  /** Sets the expression associated with the lower bound of the range slider. The value `None`
    * can be used to disable the range slider function (it will be hidden).
    *
    * If _either of_ `rangeLo` and `rangeHi` / `extent` is `None`, the range functionality is hidden.
    */
  def rangeLo_=(expr: Option[IntObj[T]])(implicit tx: T): Unit

  /** Gets the expression associated with the upper bound of the range slider (if any). */
  def rangeHi                              (implicit tx: T): Option[IntObj[T]]

  /** Sets the expression associated with the upper bound of the range slider. The value `None`
    * can be used to disable the range slider function (it will be hidden). If the value is `Some` and
    * an expression was specified for `extent`, the `extent` expression will be automatically set to
    * `None`. That is, `rangeHi` is mutually exclusive with `extent`.
    *
    * If _either of_ `rangeLo` and `rangeHi` / `extent` is `None`, the range functionality is hidden.
    */
  def rangeHi_=(expr: Option[IntObj[T]])(implicit tx: T): Unit

  /** Gets the expression associated with the extent of the range slider (if any). */
  def extent                               (implicit tx: T): Option[IntObj[T]]

  /** Sets the expression associated with the extent the range slider. The value `None`
    * can be used to disable the range slider function (it will be hidden). If the value is `Some` and
    * an expression was specified for `rangeHi`, the `rangeHi` expression will be automatically set to
    * `None`. That is, `extent` is mutually exclusive with `rangeHi`.
    *
    * If _either of_ `rangeLo` and `rangeHi` / `extent` is `None`, the range functionality is hidden.
    */
  def extent_= (expr: Option[IntObj[T]])(implicit tx: T): Unit
}