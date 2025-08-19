package mau.exec

import java.net.URLClassLoader
import java.net.URI

class RuleClassLoader(ruleUri: URI, parent: ClassLoader) extends URLClassLoader(Array(ruleUri.toURL), parent):  
  override def loadClass(name: String, resolve: Boolean): Class[?] =
    if !RuleClassLoader.isAllowed(name) then
      Console.err.println(s"Class $name is not allowed.")
      throw new ClassNotFoundException(s"Class $name is not allowed.")
    else super.loadClass(name, resolve)

object RuleClassLoader:
  private val allowedList = Seq(
    "java\\.lang\\.(Object|String|Boolean|Byte|Character|Short|Integer|Long|Float|Double|Math|Number|Enum|Throwable|Exception|RuntimeException|Error)".r,
    "java\\.lang\\.invoke\\.LambdaMetafactory".r,
    "java\\.io\\.Serializable".r, // Scala objects extend Serializable
    "scala\\.(Option|Some\\$?|None\\$|Function1|PartialFunction)".r,
    "scala\\.collection\\.immutable\\..+".r,
    "scala\\.runtime\\.(AbstractPartialFunction|BoxesRunTime|Nothing\\$?|ScalaRunTime\\$)".r,
    "mau\\.round\\..+".r,
    "mau\\.engine\\.(Rule|InMemoryRule\\$)".r
  )
  
  private def isAllowed(name: String): Boolean =
    allowedList.exists(p => p.matches(name))
