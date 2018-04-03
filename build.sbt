lazy val baseName  = "LucreSwing"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "1.8.0-SNAPSHOT"
lazy val mimaVersion    = "1.8.0"

name                 := baseName
version              := projectVersion
organization         := "de.sciss"
scalaVersion         := "2.12.5"
crossScalaVersions   := Seq("2.12.5", "2.11.12")
description          := "Swing support for Lucre, and common views"
homepage             := Some(url(s"https://github.com/Sciss/${name.value}"))
licenses             := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val lucre     = "3.6.0-SNAPSHOT"
    val desktop   = "0.8.0"
    val widgets   = "1.11.1"
    val treeTable = "1.3.9"
    val model     = "0.3.4"
  }
  val test = new {
    val fileUtil  = "1.1.2"  // sbt chokes on 1.1.3
    val submin    = "0.2.2"
  }
}

resolvers += "Oracle Repository" at "http://download.oracle.com/maven" // required for lucrestm-bdb

libraryDependencies ++= Seq(
  "de.sciss" %% "lucre-expr"         % deps.main.lucre,
  "de.sciss" %% "desktop"            % deps.main.desktop,
  "de.sciss" %% "audiowidgets-swing" % deps.main.widgets,   // TODO: should be possible to just depend on the range slider
  "de.sciss" %% "treetable-scala"    % deps.main.treeTable, // TODO: should be going into a dedicated sub-project?
  "de.sciss" %% "model"              % deps.main.model,
  "de.sciss" %% "lucre-bdb"          % deps.main.lucre    % "test",
  "de.sciss" %% "fileutil"           % deps.test.fileUtil % "test",
  "de.sciss" %  "submin"             % deps.test.submin   % "test"
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
