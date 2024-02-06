package com.github.nvelychenko.drupalextend.reference.referenceProvider

import com.github.nvelychenko.drupalextend.extensions.getAllProjectKeys
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.index.ContentEntityFqnIndex
import com.github.nvelychenko.drupalextend.project.drupalExtendSettings
import com.github.nvelychenko.drupalextend.reference.referenceType.FieldPropertyReference
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import com.intellij.util.indexing.FileBasedIndex
import com.jetbrains.php.lang.psi.elements.*

class FieldReferenceProvider : PsiReferenceProvider() {

    /**
     * Finds $node->get('field_body')
     */
    override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
        val project = element.project
        if (!project.drupalExtendSettings.isEnabled) return emptyArray()

        val psiReferences = PsiReference.EMPTY_ARRAY

        element as StringLiteralExpression
        val parameterList = (element.parent as ParameterList)
        val methodReference = (parameterList.parent as MethodReference)

        if (methodReference.name != "get") return psiReferences

        val reference = when (val classReference = methodReference.classReference) {
            is PhpReference -> classReference
            is ArrayAccessExpression -> classReference.value as? PhpReference
            else -> null
        } ?: return psiReferences

        val types = reference.globalType.filterPrimitives().types

        val index = FileBasedIndex.getInstance()

        val contentEntity = index.getAllProjectKeys(ContentEntityFqnIndex.KEY, project)
            .find { types.contains(it) } ?: return psiReferences

        val entityTypeId =
            index.getValue(ContentEntityFqnIndex.KEY, contentEntity, project)?.entityTypeId
                ?: return psiReferences

        return arrayOf(FieldPropertyReference(element, entityTypeId, element.contents))

    }

}