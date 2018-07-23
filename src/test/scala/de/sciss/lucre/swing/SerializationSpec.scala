package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.serial.{DataInput, DataOutput}
import org.scalatest.{FlatSpec, Matchers}

class SerializationSpec extends FlatSpec with Matchers {
  "An graph" should "be serializable" in {
    import ExOps._
    import graph._
    val gIn = Graph {
      val sl    = Slider.mk { w =>
        w.min   = 1
        w.max   = 10
        w.value = 1
      }
      val txt = (sl.value.dbAmp * 2.0).toStr
      val lb = Label(txt)
      FlowPanel(sl, lb)
    }

    val out = DataOutput()
    Graph.serializer.write(gIn, out)
    val in = DataInput(out.toByteArray)
    val gOut = Graph.serializer.read(in)

    assert(gIn === gOut)
  }
}