package mau.exec

import mau.exec.RuleSource

import java.net.URI
import java.nio.file.*
import scala.util.Properties

class Workspace(val root: Path):
  def getVirtualSourceFile(className: String): URI =
    root.resolve(className + ".scala").toUri

  def createDirectoryAndSourceFile(source: RuleSource): (Path, Path) =
    val dir = root.resolve(s"rule-${source.uid}")
    val sourceFile = dir.resolve(source.className + ".scala")
    Files.createDirectory(dir)
    Files.write(sourceFile, source.wrappedImpl.getBytes)
    (dir, sourceFile)

  def getRuleUri(ruleId: String): URI =
    root.resolve(s"rule-$ruleId").toUri

object Workspace:
  def init(): Workspace =
    val root = Paths.get(Properties.userDir, ".mau")
    if !Files.exists(root) then Files.createDirectory(root)
    Workspace(root)