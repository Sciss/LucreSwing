# LucreSwing

[![Build Status](https://travis-ci.org/Sciss/LucreSwing.svg?branch=master)](https://travis-ci.org/Sciss/LucreSwing)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.11)

## statement

LucreSwing is a Scala library which bridges between Swing (through 
the [Desktop](https://git.iem.at/sciss/Desktop/) project) and [Lucre](https://git.iem.at/sciss/Lucre/).
It is (C)opyright 2014&ndash;2018 by Hanns Holger Rutz. All rights reserved. The project is released under
the [GNU Lesser General Public License](https://git.iem.at/sciss/LucreSwing/raw/master/LICENSE) v2.1+ and comes 
with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / building

This project compiles against Scala 2.12, 2.11 using sbt.

To use the library in your project:

    "de.sciss" %% "lucreswing" % v

The current version `v` is `"1.15.1"`.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
