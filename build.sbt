lazy val baseName   = "Lucre-Swing"
lazy val baseNameL  = baseName.toLowerCase
lazy val gitProject = "LucreSwing"

lazy val projectVersion = "2.6.2"
lazy val mimaVersion    = "2.6.0"

// ---- dependencies ----

lazy val deps = new {
  val main = new {
    val desktop   = "0.11.3"
    val laminar   = "0.11.0"
    val lucre     = "4.4.2"
    val model     = "0.3.5"
    val swingPlus = "0.5.0"
    val treeTable = "1.6.1"
    val widgets   = "2.3.2"
  }
  val test = new {
    val fileUtil  = "1.1.5"
    val scalaTest = "3.2.4"
    val submin    = "0.3.4"
  }
}

lazy val commonJvmSettings = Seq(
  crossScalaVersions   := Seq("3.0.0-RC1", "2.13.4", "2.12.13"),
)

// sonatype plugin requires that these are in global
ThisBuild / version      := projectVersion
ThisBuild / organization := "de.sciss"

lazy val root = crossProject(JVMPlatform, JSPlatform).in(file("."))
  .jvmSettings(commonJvmSettings)
  .settings(
    name                 := baseName,
//    version              := projectVersion,
//    organization         := "de.sciss",
    scalaVersion         := "2.13.4",
    description          := "Swing support for Lucre, and common views",
    homepage             := Some(url(s"https://git.iem.at/sciss/$gitProject")),
    licenses             := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
    libraryDependencies ++= Seq(
      "de.sciss"      %%% "lucre-expr"   % deps.main.lucre,
      "de.sciss"      %%% "model"        % deps.main.model,
      "org.scalatest" %%% "scalatest"    % deps.test.scalaTest % Test,
    ),
    scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature", "-encoding", "utf8"),
    scalacOptions ++= {
      if (isDotty.value) Nil else Seq("-Xlint", "-Xsource:2.13")
    },
    scalacOptions in (Compile, compile) ++= (if (!isDotty.value && scala.util.Properties.isJavaAtLeast("9")) Seq("-release", "8") else Nil), // JDK >8 breaks API; skip scala-doc
    // ---- compatibility ----
    mimaPreviousArtifacts := Set("de.sciss" %% baseNameL % mimaVersion),
    updateOptions := updateOptions.value.withLatestSnapshots(false)
  )
  .jvmSettings(
    libraryDependencies ++= Seq(
      "de.sciss" %% "desktop"            % deps.main.desktop,
      "de.sciss" %% "audiowidgets-swing" % deps.main.widgets,   // TODO: should be possible to just depend on the range slider
      "de.sciss" %% "swingplus"          % deps.main.swingPlus,
      "de.sciss" %% "treetable-scala"    % deps.main.treeTable, // TODO: should be going into a dedicated sub-project?
      "de.sciss" %% "lucre-bdb"          % deps.main.lucre     % Test,
      "de.sciss" %% "fileutil"           % deps.test.fileUtil  % Test,
      "de.sciss" %  "submin"             % deps.test.submin    % Test,
    ),
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "com.raquo" %%% "laminar" % deps.main.laminar,
    )
  )
  .settings(publishSettings)

// ---- publishing ----
lazy val publishSettings = Seq(
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  developers := List(
    Developer(
      id    = "sciss",
      name  = "Hanns Holger Rutz",
      email = "contact@sciss.de",
      url   = url("https://www.sciss.de")
    )
  ),
  scmInfo := {
    val h = "git.iem.at"
    val a = s"sciss/$gitProject"
    Some(ScmInfo(url(s"https://$h/$a"), s"scm:git@$h:$a.git"))
  },
)

