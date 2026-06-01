package com.github.andrelmv.kotbox.proto.generator

import com.github.andrelmv.kotbox.utils.isDataClass
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference

/**
 * Resolves Kotlin class references via PSI lookups.
 *
 * Resolution strategy (in priority order):
 * 1. Explicit import in the containing file
 * 2. Same package as the containing file
 * 3. Project-wide short name lookup
 *
 * Remaining limitation: wildcard imports (import com.example.*) are not resolved.
 */
internal class KotlinClassFinder(
    private val project: Project,
    private val scope: GlobalSearchScope,
) {
    fun findDataClass(
        name: String,
        typeReference: KtTypeReference,
    ): KtClass? = findKtClass(name, typeReference)?.takeIf { it.isDataClass() }

    fun findEnumClass(
        name: String,
        typeReference: KtTypeReference,
    ): KtClass? = findKtClass(name, typeReference)?.takeIf { it.isEnum() }

    private fun findKtClass(
        name: String,
        typeReference: KtTypeReference,
    ): KtClass? {
        val file = typeReference.containingFile as? KtFile

        return findByExplicitImport(name, file)
            ?: findBySamePackage(name, file)
            ?: findByProjectWideLookup(name)
    }

    private fun findByExplicitImport(
        name: String,
        file: KtFile?,
    ): KtClass? =
        file
            ?.importDirectives
            ?.mapNotNull { it.importedFqName?.asString() }
            ?.firstOrNull { it.endsWith(".$name") }
            ?.let { findKtClassByExactFqn(it) }

    private fun findBySamePackage(
        name: String,
        file: KtFile?,
    ): KtClass? = file?.let { findKtClassByExactFqn("${it.packageFqName.asString()}.$name") }

    private fun findByProjectWideLookup(name: String): KtClass? =
        findKotlinLightClassesByName(name).firstNotNullOfOrNull { it.kotlinOrigin as? KtClass }

    private fun findKtClassByExactFqn(fqn: String): KtClass? {
        val shortName = fqn.substringAfterLast(".")
        return findKotlinLightClassesByName(shortName)
            .mapNotNull { it.kotlinOrigin as? KtClass }
            .firstOrNull { it.fqName?.asString() == fqn }
    }

    private fun findKotlinLightClassesByName(name: String) =
        PsiShortNamesCache
            .getInstance(project)
            .getClassesByName(name, scope)
            .filterIsInstance<KtLightClass>()
}
