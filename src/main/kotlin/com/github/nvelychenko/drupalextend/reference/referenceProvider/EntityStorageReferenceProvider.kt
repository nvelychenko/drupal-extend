package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.isSuperInterfaceOf
import com.github.nvelychenko.drupalextend.reference.referenceType.DrupalStorageEntityReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.jetbrains.php.PhpIndex
import com.jetbrains.php.lang.psi.elements.Method
import com.jetbrains.php.lang.psi.elements.MethodReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class EntityStorageReferenceProvider : PsiReferenceProvider() {

    /**
     * Finds \Drupal::entityTypeManager()->getStorage('ENTITY_TYPE') and adds reference to it
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val psiReferences = PsiReference.EMPTY_ARRAY

        element as StringLiteralExpression

        if (element.contents.isEmpty()) return psiReferences

        val methodReference = element.parent.parent as MethodReference

        if (methodReference.name != "getStorage") return psiReferences

        val methodClass = (methodReference.resolve() as? Method)?.containingClass ?: return psiReferences

        val entityTypeManagerInterface =
            PhpIndex.getInstance(element.project)
                .getAnyByFQN("\\Drupal\\Core\\Entity\\EntityTypeManagerInterface").firstOrNull()
                ?: return psiReferences

        return if (methodClass.isSuperInterfaceOf(entityTypeManagerInterface)) {
            arrayOf(DrupalStorageEntityReference(element, element.contents))
        } else psiReferences
    }

}