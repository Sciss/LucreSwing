package de.sciss.lucre.swing

import de.sciss.serial.{DataInput, DataOutput}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class SerializationSpec extends AnyFlatSpec with Matchers {
  "An graph" should "be serializable" in {
    import graph._
    val gIn = Graph {
      val sl      = Slider()
      sl.min      = 1
      sl.max      = 10
      sl.value()  = 1
      val txt     = (sl.value().dbAmp * 2.0).toStr
      val lb      = Label(txt)
      val flow    = FlowPanel(sl, lb)
      val bp      = BorderPanel(north = flow, center = CheckBox("Checking one two"))
      bp
    }

    val out = DataOutput()
    Graph.format.write(gIn, out)
    val in = DataInput(out.toByteArray)
    val gOut = Graph.format.read(in)

    assert(gIn === gOut)
  }

  "Another graph" should "be serializable" in {
    import graph._
    val gIn = Graph {
      val contents0 = (1 to 3).flatMap { i =>
        val sl      = Slider()
        sl.min      = 1
        sl.max      = 10
        sl.value()  = i * 3
        val lb      = Label(s"Slider $i:")
        lb.hAlign   = Align.Trailing
        lb :: sl :: Nil
      }
      val cb        = CheckBox("Disabled")
      val slE       = Slider()
      slE.enabled   = !cb.selected()
      val flow      = FlowPanel(ComboBox(List(1, 2, 3)))
      flow.align    = Align.Leading
      flow.hGap     = 0
      val contents  = contents0 ++ List(cb, slE, Label("Combo:"), flow)

      val p = GridPanel(contents: _*)
      p.columns = 2
      p.compactColumns = true
      p.border = Border.Empty(4, 8, 4, 8)
      p
    }

    val out = DataOutput()
    Graph.format.write(gIn, out)
    val in = DataInput(out.toByteArray)
    val gOut = Graph.format.read(in)

    assert(gIn === gOut)
  }
}