package edu.cmu.cs.alabaster

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyQualifiedNameOwner
import com.jetbrains.python.psi.search.PyProjectScopeBuilder
import com.jetbrains.python.psi.stubs.PyClassNameIndex
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex

object PsiElementFinder {

  private val prevFinds = mutableMapOf<Pair<Project, String>, PsiElement?>()

  operator fun get(project: Project, name: String): PsiElement? {
    val finds = prevFinds
    return finds.computeIfAbsent(Pair(project, name)) { findElement(it.first, it.second) }
  }

  private fun findElement(project: Project, name: String): PyElement? {
    val elements = mutableListOf<PyQualifiedNameOwner>()
    val transformName = name.substringAfterLast('.')
    val scope = PyProjectScopeBuilder.excludeSdkTestsScope(project)
    elements.addAll(PyFunctionNameIndex.find(transformName, project, scope))
    elements.addAll(PyClassNameIndex.find(transformName, project, scope))
    return elements.firstOrNull { it.qualifiedName == name }
  }

}
