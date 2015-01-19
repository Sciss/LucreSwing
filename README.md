# LucreSwing

## statement

LucreSwing is a Scala library which bridges between Swing (through the [Desktop](https://github.com/Sciss/Desktop/) project) and [LucreEvent](https://github.com/Sciss/LucreEvent/). It is (C)opyright 2014&ndash;2015 by Hanns Holger Rutz. All rights reserved. The project is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/LucreSwing/master/LICENSE) v2.1+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / building

This project compiles against Scala 2.11, 2.10 using sbt 0.13.

To use the library in your project:

    "de.sciss" %% "lucreswing" % v

The current version `v` is `"0.7.0"`.

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
