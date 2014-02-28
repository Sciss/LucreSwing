name         := "LucreSwing"

version      := "0.1.0-SNAPSHOT"

organization := "de.sciss"

scalaVersion := "2.10.3"

description  := "Swing support for Lucre, and common views"

homepage     := Some(url("https://github.com/Sciss/" + name.value))

licenses     := Seq("GPL v2+" -> url("http://www.gnu.org/licenses/gpl-2.0.txt"))

lazy val eventVersion     = "2.6.+"

lazy val desktopVersion   = "0.4.2+"

// lazy val treeTableVersion = "1.3.4+"

libraryDependencies ++= Seq(
  "de.sciss" %% "lucreevent" % eventVersion,
  "de.sciss" %% "desktop"    % desktopVersion
  // "de.sciss" %% "treetable-scala" % treeTableVersion
  // "org.scala-lang" %  "scala-swing"     % scalaVersion.value
)

// retrieveManaged := true

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

// ---- publishing ----

publishMavenStyle := true

publishTo :=
  Some(if (version.value endsWith "-SNAPSHOT")
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

