package mau.exec

import org.eclipse.lsp4j.Location

import java.net.URI
import java.util as ju
import scala.meta.pc.ContentType
import scala.meta.pc.ParentSymbols
import scala.meta.pc.SymbolDocumentation
import scala.meta.pc.SymbolSearch
import scala.meta.pc.SymbolSearch.Result
import scala.meta.pc.SymbolSearchVisitor

object SymbolSearchImpl extends SymbolSearch:
  override def documentation(symbol: String, parents: ParentSymbols): ju.Optional[SymbolDocumentation] = 
    // println(s"documentation $symbol $parents")
    notImplemented("documentation")

  override def documentation(
      symbol: String,
      parents: ParentSymbols,
      docstringContentType: ContentType,
  ): ju.Optional[SymbolDocumentation] =
    // println(s"documentation $symbol $parents $docstringContentType")
    notImplemented("documentation 2")

  override def definition(x: String, source: URI): ju.List[Location] = 
    notImplemented("definition")

  override def definitionSourceToplevels(sym: String, source: URI): ju.List[String] =
    notImplemented("definitionSourceToplevels")

  override def search(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): Result =
    // println(s"search: $query $buildTargetIdentifier $visitor")
    notImplemented("search")

  override def searchMethods(query: String, buildTargetIdentifier: String, visitor: SymbolSearchVisitor): Result =
    Result.COMPLETE

  private def notImplemented(name: String) = throw new NotImplementedError(name)

