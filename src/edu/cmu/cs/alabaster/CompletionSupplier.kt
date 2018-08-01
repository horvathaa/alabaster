package edu.cmu.cs.alabaster

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.TokenType
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyElementTypes
import com.jetbrains.python.PyTokenTypes

class CompletionSupplier(
  private val transformSupplier: (List<String>) -> List<LookupElement>,
  private val parameterSupplier: (String) -> List<LookupElement>,
  private val subtransformSupplier: (String) -> List<LookupElement>
) : (PsiElement) -> List<LookupElement> {

  override operator fun invoke(element: PsiElement): List<LookupElement> {
    val prev = PsiTreeUtil.getDeepestLast(element)
    return when (prev.node?.elementType) {
      PyTokenTypes.OR!! -> transformSupplier(getPrevTransforms(prev))
      PyTokenTypes.LPAR!! -> {
        val identifier = prevIgnore(prev)
        if (identifier?.node?.elementType == PyTokenTypes.IDENTIFIER!!)
          parameterSupplier(identifier.text)
        else
          emptyList()
      }
      PyTokenTypes.DOT!! -> {
        val identifier = prevIgnore(prev)
        if (identifier?.node?.elementType == PyTokenTypes.IDENTIFIER!!)
          subtransformSupplier(identifier.text)
        else
          emptyList()
      }
      else -> emptyList()
    }

  }

  private tailrec fun prevIgnore(element: PsiElement): PsiElement? {
    val prev = PsiTreeUtil.prevLeaf(element) ?: return null
    if (prev.node?.elementType !in ignored)
      return prev
    return prevIgnore(prev)
  }

  private val ignored = setOf(TokenType.WHITE_SPACE!!, TokenType.ERROR_ELEMENT!!)

  private fun getPrevTransforms(element: PsiElement): List<String> {
    val parentExpression = getParentBinaryExpression(element) ?: return listOf()
    val result = mutableListOf<String>()
    getTopBinaryExpression(parentExpression).accept(TransformVisitor(result))
    return result
  }

  private tailrec fun getParentBinaryExpression(element: PsiElement): PsiElement? {
    if (element.node?.elementType == PyElementTypes.BINARY_EXPRESSION)
      return element
    val parent = element.parent ?: return null
    return getParentBinaryExpression(parent)
  }

  private tailrec fun getTopBinaryExpression(element: PsiElement): PsiElement {
    val parent = element.parent
    return if (parent?.node?.elementType == PyElementTypes.BINARY_EXPRESSION)
      return getTopBinaryExpression(parent)
    else element
  }

  private class TransformVisitor(private val list: MutableList<String>) : PsiElementVisitor() {
    override fun visitElement(element: PsiElement) {
      when (element.node?.elementType) {
        PyElementTypes.CALL_EXPRESSION -> element.firstChild?.let { visitElement(it) }
        PyElementTypes.REFERENCE_EXPRESSION -> {
          val parentType = element.parent?.node?.elementType
          if (parentType == PyElementTypes.CALL_EXPRESSION)
            list.add(element.text)
        }
        PyElementTypes.BINARY_EXPRESSION -> {
          element.firstChild?.let { visitElement(it) }
          element.lastChild?.let { visitElement(it) }
        }
      }
    }
  }
}
