lazy val baseName  = "LucreSwing"
lazy val baseNameL = baseName.toLowerCase

lazy val projectVersion = "1.14.0-SNAPSHOT"
lazy val mimaVersion    = "1.14.0"

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val desktop   = "0.10.0-SNAPSHOT"
    val lucre     = "3.11.0-SNAPSHOT"
    val model     = "0.3.4"
    val swingPlus = "0.4.0-SNAPSHOT"
    val treeTable = "1.5.0-SNAPSHOT"
    val widgets   = "1.14.0-SNAPSHOT"
  }
  val test = new {
    val fileUtil  = "1.1.3"
    val scalaTest = "3.0.5"
    val submin    = "0.2.2"
  }
}

lazy val root = project.withId(baseNameL).in(file("."))
  .settings(
    name                 := baseName,
    version              := projectVersion,
    organization         := "de.sciss",
    scalaVersion         := "2.13.0-M5",
    crossScalaVersions   := Seq("2.12.8", "2.11.12", "2.13.0-M5"),
    description          := "Swing support for Lucre, and common views",
    homepage             := Some(url(s"https://git.iem.at/sciss/${name.value}")),
    licenses             := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
    resolvers += "Oracle Repository" at "http://download.oracle.com/maven", // required for lucrestm-bdb
    libraryDependencies ++= Seq(
      "de.sciss"      %% "lucre-expr"         % deps.main.lucre,
      "de.sciss"      %% "desktop"            % deps.main.desktop,
      "de.sciss"      %% "audiowidgets-swing" % deps.main.widgets,   // TODO: should be possible to just depend on the range slider
      "de.sciss"      %% "swingplus"          % deps.main.swingPlus,
      "de.sciss"      %% "treetable-scala"    % deps.main.treeTable, // TODO: should be going into a dedicated sub-project?
      "de.sciss"      %% "model"              % deps.main.model,
      "de.sciss"      %% "lucre-bdb"          % deps.main.lucre     % Test,
      "de.sciss"      %% "fileutil"           % deps.test.fileUtil  % Test,
      "de.sciss"      %  "submin"             % deps.test.submin    % Test
    ),
    libraryDependencies += {
      val v = if (scalaVersion.value == "2.13.0-M5") "3.0.6-SNAP5" else deps.test.scalaTest
      "org.scalatest" %% "scalatest" % v % Test
    },
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8", "-Xfuture", "-Xlint"),
    // ---- compatibility ----
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    updateOptions := updateOptions.value.withLatestSnapshots(false)
  )
  .settings(publishSettings)

// ---- publishing ----
lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    Some(if (isSnapshot.value)
      "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    else
      "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
    )
  },
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  pomExtra := { val n = name.value
<scm>
  <url>git@git.iem.at:sciss/{n}.git</url>
  <connection>scm:git:git@git.iem.at:sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
</developers>
  }
)
