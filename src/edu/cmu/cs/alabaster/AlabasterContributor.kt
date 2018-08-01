package edu.cmu.cs.alabaster

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.patterns.PythonPatterns
import edu.cmu.cs.alabaster.config.CompletionConfig
import edu.cmu.cs.alabaster.config.Configuration
import java.io.IOException

class AlabasterContributor : CompletionContributor() {

  init {
    val javaClass = AlabasterContributor::class.java
    val config = javaClass.getResourceAsStream("/completions.json")?.use {
      Configuration(it)
    } ?: throw IOException("Unable to read config file")
    extend(CompletionType.BASIC, PythonPatterns.psiElement(), AlabasterProvider(config))
  }

  override fun invokeAutoPopup(p: PsiElement, typeChar: Char): Boolean {
    return typeChar == '|'
  }

  private class AlabasterProvider(private val config: Configuration) :
    CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
      parameters: CompletionParameters,
      c: ProcessingContext?,
      results: CompletionResultSet
    ) {
      val psi = parameters.originalFile.findElementAt(parameters.offset - 1) ?: return

      val project = parameters.editor.project ?: return

      val completions = CompletionSupplier(
        { prioritize(config.transforms, project) },
        { prioritize(config.parameters[it] ?: emptyList(), project) },
        { prioritize(config.subtransforms[it] ?: emptyList(), project) }
      )

      results.addAllElements(completions(psi))
    }

    private fun prioritize(completions: List<CompletionConfig>, project: Project)
        : List<LookupElement> {
      return completions.mapIndexed { index, completion ->
        AlabasterLookup(completion, project).prioritize(index)
      }
    }

  }

}
