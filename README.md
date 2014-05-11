# LucreSwing

## statement

LucreSwing is a Scala library which bridges between Swing (through the [Desktop](https://github.com/Sciss/Desktop/) project) and [LucreEvent](https://github.com/Sciss/LucreEvent/). It is (C)opyright 2014 by Hanns Holger Rutz. All rights reserved. The project is released under the [GNU General Public License](https://raw.github.com/Sciss/ScalaAudioFile/master/LICENSE) v2+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / building

ScalaAudioFile currently compiles against Scala 2.11, 2.10 using sbt 0.13. Because of a compatibility problem with generified Java Swing, one must currently compile using JDK 6 and not JDK 7 or newer. If you see an `IndexOutOfBoundsException` in the scalac typer, this is because you are not running on JDK 6. The compiled library of course works with any Java version.

To use the library in your project:

    "de.sciss" %% "lucreswing" % v

The current version `v` is `"0.3.0"`

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
