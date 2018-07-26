package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.serial.{DataInput, DataOutput}
import org.scalatest.{FlatSpec, Matchers}

class SerializationSpec extends FlatSpec with Matchers {
  "An graph" should "be serializable" in {
    import ExOps._
    import graph._
    val gIn = Graph {
      val sl    = Slider()
      sl.min    = 1
      sl.max    = 10
      sl.value  = 1
      val txt   = (sl.value.dbAmp * 2.0).toStr
      val lb    = Label(txt)
      val flow  = FlowPanel(sl, lb)
      val bp    = BorderPanel(north = flow, center = CheckBox("Checking one two"))
      bp
    }

    val out = DataOutput()
    Graph.serializer.write(gIn, out)
    val in = DataInput(out.toByteArray)
    val gOut = Graph.serializer.read(in)

    assert(gIn === gOut)
  }
}