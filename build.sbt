import org.scalajs.linker.interface.ModuleSplitStyle
import org.scalajs.sbtplugin.Stage
import scala.sys.process.*

inThisBuild(
  Seq(
    scalaVersion := "3.7.1",
    libraryDependencies += "org.scalameta" %%% "munit" % "1.1.0" % Test
  )
)

lazy val core = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("core"))
  .settings(
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % "0.14.12",
      "io.circe" %%% "circe-parser" % "0.14.12"
    )
  )

lazy val engine = project
  .in(file("engine"))
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
      "org.scala-lang" %% "scala3-presentation-compiler" % scalaVersion.value
    ),
    Test / fork := true
  )
  .dependsOn(core.jvm)

lazy val server = project
  .in(file("server"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "cask" % "0.10.2",
      "com.lihaoyi" %% "requests" % "0.9.0"
    ),
    Compile / resourceGenerators += Def.task {
      val _ = (webpage / Compile / fullLinkJS).value
      cmd("npm run build").!!
      (Compile / resourceManaged).value.**("*").filter(f => !f.isDirectory).get
    },
    run / fork := true,
    run / javaOptions += "-Dmau.local",
    Compile / packageDoc / mappings := Seq.empty,
  )
  .dependsOn(engine, core.jvm)

lazy val webpage = project
  .in(file("webpage"))
  .enablePlugins(ScalaJSPlugin, ScalablyTypedConverterExternalNpmPlugin)
  .settings(
    externalNpm := {
      val npmFolder = baseDirectory.value.getParentFile
      cmd("npm install").!
      npmFolder
    },
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerConfig := {
      scalaJSLinkerConfig.value
        .withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("mau")))
    },
    Compile / scalaJSStage := Stage.FullOpt,
    Compile / fullLinkJS / scalaJSLinkerConfig := {
      scalaJSLinkerConfig.value.withModuleKind(ModuleKind.ESModule)
    },
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % "2.8.0",
      "com.raquo" %%% "laminar" % "17.2.0",
      "io.github.cquiroz" %%% "scala-java-time" % "2.6.0"
    ),
    stIncludeDev := false
  )
  .dependsOn(core.js)

def isWindows: Boolean =
  System.getProperty("os.name").toLowerCase.startsWith("win")

def cmd(args: String): ProcessBuilder =
  if (isWindows) Process(s"cmd.exe /c $args") else Process(args)
