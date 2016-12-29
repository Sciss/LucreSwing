# LucreSwing

[![Flattr this](http://api.flattr.com/button/flattr-badge-large.png)](https://flattr.com/submit/auto?user_id=sciss&url=https%3A%2F%2Fgithub.com%2FSciss%2FLucreSwing&title=LucreSwing%20Library&language=Scala&tags=github&category=software)
[![Build Status](https://travis-ci.org/Sciss/LucreSwing.svg?branch=master)](https://travis-ci.org/Sciss/LucreSwing)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.11/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.sciss/lucreswing_2.11)

## statement

LucreSwing is a Scala library which bridges between Swing (through the [Desktop](https://github.com/Sciss/Desktop/) project) and [LucreEvent](https://github.com/Sciss/LucreEvent/). It is (C)opyright 2014&ndash;2017 by Hanns Holger Rutz. All rights reserved. The project is released under the [GNU Lesser General Public License](https://raw.github.com/Sciss/LucreSwing/master/LICENSE) v2.1+ and comes with absolutely no warranties. To contact the author, send an email to `contact at sciss.de`

## requirements / building

This project compiles against Scala 2.12, 2.11, 2.10 using sbt 0.13.

To use the library in your project:

    "de.sciss" %% "lucreswing" % v

The current version `v` is `"1.4.3"`.

## contributing

Please see the file [CONTRIBUTING.md](CONTRIBUTING.md)

## todo

- collapse multiple undoable edits, especially with things like slider movements which produce many subsequent and related edits
