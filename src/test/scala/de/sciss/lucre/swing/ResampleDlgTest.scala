package de.sciss.lucre.swing

import de.sciss.lucre.expr.ExOps
import de.sciss.lucre.stm.InMemory

import scala.swing.Component

object ResampleDlgTest extends AppLike {
  protected def mkView(): Component = {
    import ExOps._
    import graph._
    val g = Graph {
//      val sepWave         = TitledSeparator("Waveform I/O")
      val sepWave         = Label("———— Waveform I/O ————") // Titled-Border
      val lbIn            = Label("Input file:")
      val ggIn            = PathField()
      ggIn.mode           = PathField.Open
//      ggIn.info           = true
      val lbOut           = Label("Output file:")
      val ggOut           = PathField()
      ggOut.mode          = PathField.Save
//      val ggFileType      = ComboBox()
//      val ggSmpFmt        = ComboBox()
//      val ggGain          = NumberField()
//      ggGain.spec         = ParamSpec(...)
//      ggGain.value        = -0.20
//      ggGain.unit         = "dB"
//      val ggGainType      = ComboBox()
      val sepSRC          = Label("———— Sample Rate Conversion ————") // Titled-Border
//      val lbNewRate       = Label("New rate:")
//      val ggNewRate       = NumberField()
      val ggChangePch     = CheckBox("Change Pitch/Speed")
//      val lbFltLen        = Label("FIR length:")
//      val ggFltLen        = ComboBox()
//      val ggProg          = ProgressBar()
//      val ggCancel        = Button("X")
//      ggCancel.enabled    = false
//      val ggRender        = Button("Render")
//      ggRender.action     = "render".attr

      val lineIn  = FlowPanel(lbIn, ggIn)
      val lineOut = FlowPanel(lbOut, ggOut)
      val p = GridPanel(
        sepWave, lineIn, lineOut, sepSRC, ggChangePch
      )
      p.columns = 1
      p
    }

    type              S = InMemory
    implicit val sys: S = InMemory()

    val view = sys.step { implicit tx =>
      g.expand[S]
    }
    view.component
  }
}