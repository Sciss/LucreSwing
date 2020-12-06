# LucreSwing

[![Build Status](https://github.com/Sciss/LucreSwing/workflows/Scala%20CI/badge.svg?branch=main)](https://github.com/Sciss/LucreSwing/actions?query=workflow%3A%22Scala+CI%22)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucre-swing_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucre-swing_2.13)

## statement

LucreSwing is a Scala library which bridges between Swing (through 
the [Desktop](https://git.iem.at/sciss/Desktop/) project) and [Lucre](https://git.iem.at/sciss/Lucre/).
It is (C)opyright 2014&ndash;2020 by Hanns Holger Rutz. All rights reserved. The project is released under
the [GNU Affero General Public License](https://git.iem.at/sciss/LucreSwing/raw/main/LICENSE) v3+ and comes 
with absolutely no warranties. To contact the author, send an e-mail to `contact at sciss.de`.

## requirements / building

This project builds with sbt against Scala 2.12, 2.13, Dotty (JVM) and Scala 2.13 (JS).
The last version to support Scala 2.11 was 1.17.2.

To use the library in your project:

    "de.sciss" %% "lucre-swing" % v

The current version `v` is `"2.5.0"`.

Note that you cannot compile with the combination of Dotty and JDK 9 or greater, because the
`-release` scalac option is not supported.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## notes

The desktop application's widgets in package `de.sciss.lucre.swing` are not available on Scala.js. Instead, the
graph construction element in `de.sciss.lucre.swing.graph` are (with few unsupported exceptions) available both
for the JVM and for JS, making it thus possible to render the `Widget` contents both on the desktop and inside
the browser.

We have yet to determine how to "export" a widget to Scala.js. For an example and minimum CSS, see
[SoundProcessesJSTest](https://github.com/Sciss/SoundProcessesJSTest/blob/main/lucre-swing.css).

The following graph elements are currently not supported in Scala.js:

- `DropTarget`
- `PathField` (the peer still uses `File` instead of `URI`)

## publishing releases

There is a [bug in sbt-crossproject](https://github.com/portable-scala/sbt-crossproject/issues/130), 
when running `sbt +publishSigned` we end up with error "Repository for publishing is not specified." 
Instead, sbt has to be run with `sbt +rootJVM/publishSigned +rootJS/publishSigned`.

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
- Scala.js rendering: styling - have a look at https://css-tricks.com/custom-styling-form-inputs-with-modern-css-features/
