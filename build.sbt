lazy val baseName  = "LucreSwing"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "1.7.0-SNAPSHOT"
lazy val mimaVersion    = "1.7.0"

name                 := baseName
version              := projectVersion
organization         := "de.sciss"
scalaVersion         := "2.12.4"
crossScalaVersions   := Seq("2.12.4", "2.11.11", "2.10.6")
description          := "Swing support for Lucre, and common views"
homepage             := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses             := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

// ---- dependencies ----

lazy val lucreVersion     = "3.5.0-SNAPSHOT"
lazy val desktopVersion   = "0.8.0"
lazy val widgetsVersion   = "1.11.1"
lazy val treeTableVersion = "1.3.9"
lazy val modelVersion     = "0.3.4"

// ---- test-only ----

lazy val fileUtilVersion  = "1.1.2"  // sbt chokes on 1.1.3
lazy val subminVersion    = "0.2.2"

resolvers += "Oracle Repository" at "http://download.oracle.com/maven" // required for lucrestm-bdb

libraryDependencies ++= Seq(
  "de.sciss" %% "lucre-expr"         % lucreVersion,
  "de.sciss" %% "desktop"            % desktopVersion,
  "de.sciss" %% "audiowidgets-swing" % widgetsVersion,   // TODO: should be possible to just depend on the range slider
  "de.sciss" %% "treetable-scala"    % treeTableVersion, // TODO: should be going into a dedicated sub-project?
  "de.sciss" %% "model"              % modelVersion,
  "de.sciss" %% "lucre-bdb"          % lucreVersion    % "test",
  "de.sciss" %% "fileutil"           % fileUtilVersion % "test",
  "de.sciss" %  "submin"             % subminVersion   % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint")

// ---- compatibility ----

mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion)

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (isSnapshot.value)
    "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
  else
    "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
  )

publishArtifact in Test := false

pomIncludeRepository := { _ => false }

pomExtra := { val n = name.value
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
}
