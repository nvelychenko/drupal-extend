package com.github.nvelychenko.drupalextend.completion.providers

import com.github.nvelychenko.drupalextend.data.contentEntitySqlFactoryClass
import com.github.nvelychenko.drupalextend.extensions.getValue
import com.github.nvelychenko.drupalextend.type.EntityQueryTypeProvider.Companion.END_KEY
import com.github.nvelychenko.drupalextend.type.EntityQueryTypeProvider.Companion.KEY
import com.intellij.codeInsight.completion.CompletionResultSet
import com.jetbrains.php.lang.psi.elements.MemberReference
import com.github.nvelychenko.drupalextend.index.ContentEntityIndex.Companion.KEY as ContentEntityIndexKEY

/**
 * Provides autocompletion for fields.
 *
 * $node->get('field_|
 * $node->set('field_|
 */
open class EntityQueryConditionFieldCompletionProvider : FieldCompletionCompletionProvider() {

    override val methodsToAutocomplete = arrayOf("condition", "exists", "notExists", "sort")

    override fun processMemberReference(memberReference: MemberReference, result: CompletionResultSet) {
        val types = memberReference.globalType.types

        val signature = memberReference.signature
        if (signature.contains(END_KEY) && !types.contains(contentEntitySqlFactoryClass)) return

        val entityTypeId = signature.substringAfter(KEY).substringBefore(END_KEY)
        val project = memberReference.project
        val contentEntity = fileBasedIndex.getValue(ContentEntityIndexKEY, entityTypeId, project) ?: return
        buildResultForEntity(contentEntity, project, result)
    }

}