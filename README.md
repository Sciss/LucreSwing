# LucreSwing

[![Build Status](https://travis-ci.org/Sciss/LucreSwing.svg?branch=main)](https://travis-ci.org/Sciss/LucreSwing)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.13)

## statement

LucreSwing is a Scala library which bridges between Swing (through 
the [Desktop](https://git.iem.at/sciss/Desktop/) project) and [Lucre](https://git.iem.at/sciss/Lucre/).
It is (C)opyright 2014&ndash;2020 by Hanns Holger Rutz. All rights reserved. The project is released under
the [GNU Affero General Public License](https://git.iem.at/sciss/LucreSwing/raw/main/LICENSE) v3+ and comes 
with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / building

This project compiles against Scala 2.13, 2.12 using sbt (the last version to support Scala 2.11 is 1.17.2).

To use the library in your project:

    "de.sciss" %% "lucreswing" % v

The current version `v` is `"1.21.0"`.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
