name               := "LucreSwing"

version            := "0.9.0"

organization       := "de.sciss"

scalaVersion       := "2.11.6"

crossScalaVersions := Seq("2.11.6", "2.10.5")

description        := "Swing support for Lucre, and common views"

homepage           := Some(url("https://github.com/Sciss/" + name.value))
 
licenses           := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt"))

// ---- dependencies ----

lazy val eventVersion     = "2.7.3"

lazy val desktopVersion   = "0.7.0"

lazy val widgetsVersion   = "1.9.0"

lazy val treeTableVersion = "1.3.7"

// ---- test-only ----

lazy val stmVersion       = "2.1.1"

lazy val fileUtilVersion  = "1.1.1"

lazy val webLaFVersion    = "1.28"

resolvers += "Oracle Repository" at "http://download.oracle.com/maven" // required for lucrestm-bdb

libraryDependencies ++= Seq(
  "de.sciss" %% "lucreevent"         % eventVersion,
  "de.sciss" %% "desktop"            % desktopVersion,
  "de.sciss" %% "audiowidgets-swing" % widgetsVersion,   // TODO: should be possible to just depend on the range slider
  "de.sciss" %% "treetable-scala"    % treeTableVersion, // TODO: should be going into a dedicated sub-project?
  "de.sciss" %% "lucrestm-bdb"       % stmVersion      % "test",
  "de.sciss" %% "fileutil"           % fileUtilVersion % "test",
  "de.sciss" %  "weblaf"             % webLaFVersion   % "test"
)

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture")

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

