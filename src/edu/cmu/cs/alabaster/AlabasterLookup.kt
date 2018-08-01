package edu.cmu.cs.alabaster

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.imports.AddImportHelper
import edu.cmu.cs.alabaster.config.CompletionConfig

class AlabasterLookup(private val config: CompletionConfig, project: Project) : LookupElement() {

  private val psi = config.documentation?.let { PsiElementFinder[project, it] }

  override fun getLookupString(): String {
    return config.lookup
  }

  override fun handleInsert(context: InsertionContext?) {

    val project = context?.project ?: return

    val document = context.document
    val file = context.file

    config.import?.let {

      val from = it.from
      if (from == null)
        AddImportHelper.addImportStatement(file, it.import, null, null, null)
      else
        AddImportHelper.addOrUpdateFromImportStatement(file, from, it.import, null, null, null)

    }

    val lineStart = document.getLineStartOffset(document.getLineNumber(context.startOffset))
    if (config.newline) {

      file.findElementAt(context.startOffset)?.let {

        val pipe = prevPipe(it) ?: return@let
        if (pipe.textOffset < lineStart) return@let
        val prevPipe = prevPipe(pipe) ?: return@let
        if (prevPipe.textOffset < lineStart) return@let
        if (pipe.parent?.node?.firstChildNode?.elementType != PyElementTypes.BINARY_EXPRESSION)
          return@let
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(document)
        document.insertString(pipe.textOffset, "\\\n")

      }

    }


    insertAroundCaret(context, config.insertBefore, config.insertSelect, config.insertAfter)

    val lineEnd = document.getLineEndOffset(document.getLineNumber(context.tailOffset))
    CodeStyleManager.getInstance(project)?.reformatText(file, lineStart, lineEnd)

    if (config.popup)
      AutoPopupController.getInstance(project).scheduleAutoPopup(context.editor)

  }

  override fun getPsiElement(): PsiElement? {
    return psi
  }

  override fun renderElement(presentation: LookupElementPresentation?) {
    if (presentation == null) return
    presentation.itemText = config.lookup
    presentation.isItemTextBold = true
    presentation.setTailText(config.tail?.let { " $it" }, true)
    presentation.icon = ICON
  }

  fun prioritize(index: Int): LookupElement {
    return PrioritizedLookupElement.withPriority(this, (Integer.MAX_VALUE - index).toDouble())
  }

  private tailrec fun prevPipe(element: PsiElement): PsiElement? {
    val prev = PsiTreeUtil.prevLeaf(element) ?: return null
    if (prev.node?.elementType == PyTokenTypes.OR)
      return prev
    return prevPipe(prev)
  }

  private fun insertAroundCaret(
    context: InsertionContext,
    before: String,
    selected: String,
    after: String
  ) {
    val insert = before + selected + after
    if (insert.isEmpty()) return
    val offset = context.tailOffset
    context.document.insertString(offset, insert)
    val startOffset = offset + before.length
    context.editor.caretModel.moveToOffset(startOffset)
    context.editor.selectionModel.setSelection(startOffset, startOffset + selected.length)
  }


  private companion object {
    private val ICON = IconLoader.getIcon("/icons/beam.png")
  }

}
