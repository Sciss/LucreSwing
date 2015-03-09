package de.sciss.lucre.swing
package impl

import java.io.Serializable
import javax.swing.AbstractSpinnerModel

class NumericOptionSpinnerModel[A](value0: Option[A], minimum0: Option[A], maximum0: Option[A], stepSize0: A)
                                  (implicit num: Numeric[A])
  extends AbstractSpinnerModel with Serializable {
  
  private var _value     = value0
  private var _minimum   = minimum0
  private var _maximum   = maximum0
  private var _stepSize  = stepSize0

  def value: Option[A] = _value
  def value_=(v: Option[A]): Unit = if (_value != v) {
    _value = v
    fireStateChanged()
  }

  def minimum: Option[A] = _minimum
  def minimum_=(value: Option[A]): Unit = if (_minimum != value) {
    _minimum = value
    fireStateChanged()
  }

  def maximum: Option[A] = _maximum
  def maximum_=(value: Option[A]): Unit = if (_maximum != value) {
    _maximum = value
    fireStateChanged()
  }

  def stepSize: A = _stepSize
  def stepSize_=(value: A): Unit = if (_stepSize != value) {
    _stepSize = value
    fireStateChanged()
  }

  // dir == `true` means increase, dir == `false` means decrease
  // if value is None or will exceeds bounds, returns None else Some
  private def incrValue(dir: Boolean): Option[A] = _value.flatMap { v =>
    val newValue  = num.plus(v, if (dir) _stepSize else num.negate(_stepSize))
    val tooLarge  = maximum.exists(m => num.compare(newValue, m) > 0)
    val tooSmall  = minimum.exists(m => num.compare(newValue, m) < 0)
    if (tooLarge || tooSmall) None else Some(newValue)
  }

  /** Returns the next number in the sequence.
    *
    * @return <code>value + stepSize</code> or <code>null</code> if the sum
    *         exceeds <code>maximum</code>.
    */
  def getNextValue: AnyRef = {
    val res = incrValue(dir = true)
    if (res.isDefined) res else null
  }

  /** Returns the previous number in the sequence.
    *
    * @return <code>value - stepSize</code>, or
    *         <code>null</code> if the sum is less
    *         than <code>minimum</code>.
    */
  def getPreviousValue: AnyRef = {
    val res = incrValue(dir = false)
    if (res.isDefined) res else null
  }

  def getValue: AnyRef = _value

  /** Sets the current value for this sequence.  If <code>value</code> is
    * <code>null</code>, or not an <code>Option</code>, an
    * <code>IllegalArgumentException</code> is thrown.  No
    * bounds checking is done here.
    *
    * This method fires a <code>ChangeEvent</code> if the value has changed.
    *
    * @param v the current (non <code>null</code>) <code>Option</code>
    *          for this sequence
    * @throws IllegalArgumentException if <code>value</code> is
    *                                  <code>null</code> or not a <code>Option</code>
    */
  def setValue(v: AnyRef): Unit = {
    if ((v == null) || !v.isInstanceOf[Option[_]]) {
      val s1 = if (v == null) "" else s" (${v.getClass.getSimpleName})"
      throw new IllegalArgumentException(s"Illegal value $v$s1")
    }
    value = v.asInstanceOf[Option[A]]
  }
}