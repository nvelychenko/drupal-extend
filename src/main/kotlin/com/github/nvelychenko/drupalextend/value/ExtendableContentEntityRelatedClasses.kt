package com.github.nvelychenko.drupalextend.value

data class ExtendableContentEntityRelatedClasses(val className: String, val methodName: String) {

    companion object {
        private val allPossibleExtendableContentEntityClasses = arrayOf(
            ExtendableContentEntityRelatedClasses("\\Drupal\\Core\\Entity\\RevisionLogEntityTrait", "revisionLogBaseFieldDefinitions"),
            ExtendableContentEntityRelatedClasses("\\Drupal\\Core\\Entity\\EntityPublishedTrait", "publishedBaseFieldDefinitions"),
            ExtendableContentEntityRelatedClasses("\\Drupal\\user\\EntityOwnerTrait", "ownerBaseFieldDefinitions"),
            ExtendableContentEntityRelatedClasses("\\Drupal\\Core\\Entity\\ContentEntityBase", "baseFieldDefinitions"),
            ExtendableContentEntityRelatedClasses("\\Drupal\\Core\\Entity\\EditorialContentEntityBase", "baseFieldDefinitions"),
            ExtendableContentEntityRelatedClasses("\\Drupal\\Core\\Entity\\RevisionableContentEntityBase", "baseFieldDefinitions"),
        )

        fun getClass(clazz: String) =
            allPossibleExtendableContentEntityClasses.find { it.className == clazz }


        fun hasClass(clazz: String) =
            allPossibleExtendableContentEntityClasses.find { it.className == clazz } != null

        fun getAllPossibleExtendableClasses() =
            allPossibleExtendableContentEntityClasses.map { it.className }.toTypedArray()
    }
}
