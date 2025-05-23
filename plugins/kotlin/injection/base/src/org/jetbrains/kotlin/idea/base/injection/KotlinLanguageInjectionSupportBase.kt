// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.base.injection

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiTreeUtil.getDeepestLast
import com.intellij.util.Consumer
import com.intellij.util.Processor
import org.intellij.plugins.intelliLang.Configuration
import org.intellij.plugins.intelliLang.inject.*
import org.intellij.plugins.intelliLang.inject.config.BaseInjection
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.psi.psiUtil.startOffset

@NonNls
internal val KOTLIN_SUPPORT_ID = "kotlin"

@ApiStatus.Internal
abstract class KotlinLanguageInjectionSupportBase : AbstractLanguageInjectionSupport() {
    override fun getId(): String = KOTLIN_SUPPORT_ID

    override fun createAddActions(project: Project?, consumer: Consumer<in BaseInjection>?): Array<AnAction> {
        return super.createAddActions(project, consumer).apply {
            forEach {
                it.templatePresentation.icon = KotlinFileType.INSTANCE.icon
            }
        }
    }

    override fun isApplicableTo(host: PsiLanguageInjectionHost?): Boolean = (host as? KtElement)?.isWritable == true

    override fun useDefaultInjector(host: PsiLanguageInjectionHost?): Boolean = false

    override fun addInjectionInPlace(language: Language?, host: PsiLanguageInjectionHost?): Boolean {
        if (language == null || host == null) return false

        val configuration = Configuration.getProjectInstance(host.project).advancedConfiguration
        if (!configuration.isSourceModificationAllowed) {
            // It's not allowed to modify code without explicit permission. Postpone adding @Inject or comment till it granted.
            host.putUserData(InjectLanguageAction.FIX_KEY, Processor { fixHost: PsiLanguageInjectionHost? ->
                fixHost != null && addInjectionInstructionInCode(language, fixHost)
            })
            return false
        }

        if (!addInjectionInstructionInCode(language, host)) {
            return false
        }

        TemporaryPlacesRegistry.getInstance(host.project).addHostWithUndo(host, InjectedLanguage.create(language.id))
        return true
    }

    override fun removeInjectionInPlace(psiElement: PsiLanguageInjectionHost?): Boolean {
        if (psiElement == null || psiElement !is KtElement) return false

        val project = psiElement.getProject()

        val injectInstructions = listOfNotNull(
            findAnnotationInjection(psiElement),
            findInjectionComment(psiElement)
        )

        TemporaryPlacesRegistry.getInstance(project).removeHostWithUndo(project, psiElement)

        project.executeWriteCommand(KotlinBaseInjectionBundle.message("command.action.remove.injection.in.code.instructions")) {
            injectInstructions.forEach(PsiElement::delete)
        }

        return true
    }

    override fun findCommentInjection(host: PsiElement, commentRef: Ref<in PsiElement>?): BaseInjection? {
        // Do not inject through CommentLanguageInjector, because it injects as simple injection.
        // We need to behave special for interpolated strings.
        return null
    }

    fun findCommentInjection(host: KtElement): BaseInjection? {
        return InjectorUtils.findCommentInjection(host, "", null)
    }

    private fun findInjectionComment(host: KtElement): PsiComment? {
        val commentRef = Ref.create<PsiElement>(null)
        InjectorUtils.findCommentInjection(host, "", commentRef) ?: return null

        return commentRef.get() as? PsiComment
    }

    @ApiStatus.Internal
    fun findAnnotationInjectionLanguageId(host: KtElement): InjectionInfo? =
        findAnnotationInjection(host)?.let(::toInjectionInfo)

    @ApiStatus.Internal
    fun toInjectionInfo(annotationEntry: KtAnnotationEntry): InjectionInfo? {
        val extractLanguageFromInjectAnnotation = extractLanguageFromInjectAnnotation(annotationEntry) ?: return null
        val prefix = extractStringArgumentByName(annotationEntry, "prefix")
        val suffix = extractStringArgumentByName(annotationEntry, "suffix")
        return InjectionInfo(extractLanguageFromInjectAnnotation, prefix, suffix)
    }

    private fun findAnnotationInjection(host: KtElement): KtAnnotationEntry? {
        val modifierListOwner = findElementToInjectWithAnnotation(host) ?: return null

        val modifierList = modifierListOwner.modifierList ?: return null

        // Host can't be before annotation
        if (host.startOffset < modifierList.endOffset) return null

        return modifierListOwner.findAnnotation(FqName(AnnotationUtil.LANGUAGE))
    }

    private fun addInjectionInstructionInCode(language: Language, host: PsiLanguageInjectionHost): Boolean {
        val ktHost = host as? KtElement ?: return false
        val project = ktHost.project

        // Find the place where injection can be stated with annotation or comment
        val modifierListOwner = findElementToInjectWithAnnotation(ktHost)

        if (modifierListOwner != null && modifierListOwner.isWritable && canInjectWithAnnotation(ktHost)) {
            project.executeWriteCommand(KotlinBaseInjectionBundle.message("command.action.add.injection.annotation")) {
                modifierListOwner.addAnnotation(FqName(AnnotationUtil.LANGUAGE), "\"${language.id}\"")
            }

            return true
        }

        // Find the place where injection can be done with one-line comment
        val commentBeforeAnchor: PsiElement =
            modifierListOwner?.takeIf(PsiElement::isWritable)?.firstNonCommentChild()
                ?: findElementToInjectWithComment(ktHost)
                ?: return false

        val psiFactory = KtPsiFactory(project)
        val injectComment = psiFactory.createComment("// language=\"${language.id}\"")

        project.executeWriteCommand(KotlinBaseInjectionBundle.message("command.action.add.injection.comment")) {
            commentBeforeAnchor.parent.addBefore(injectComment, commentBeforeAnchor)
        }

        return true
    }

    abstract fun KtModifierListOwner.findAnnotation(name: FqName): KtAnnotationEntry?

    abstract fun KtModifierListOwner.addAnnotation(name: FqName, templateString: String)
}

private fun extractStringArgumentByName(annotationEntry: KtAnnotationEntry, name: String): String? {
    val namedArgument: ValueArgument =
        annotationEntry.valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == name } ?: return null
    return extractStringValue(namedArgument)
}


private fun extractLanguageFromInjectAnnotation(annotationEntry: KtAnnotationEntry): String? {
    val firstArgument: ValueArgument = annotationEntry.valueArguments.firstOrNull() ?: return null
    return extractStringValue(firstArgument)
}

private fun extractStringValue(valueArgument: ValueArgument): String? {
    val firstStringArgument = valueArgument.getArgumentExpression() as? KtStringTemplateExpression ?: return null
    val firstStringEntry = firstStringArgument.entries.singleOrNull() ?: return null

    return firstStringEntry.text
}

private fun canInjectWithAnnotation(host: PsiElement): Boolean {
    val module = ModuleUtilCore.findModuleForPsiElement(host) ?: return false
    val javaPsiFacade = JavaPsiFacade.getInstance(module.project)

    return javaPsiFacade.findClass(AnnotationUtil.LANGUAGE, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)) != null
}

private fun findElementToInjectWithAnnotation(host: KtElement): KtModifierListOwner? {
    PsiTreeUtil.getParentOfType(
        host,
        KtModifierListOwner::class.java,
        false, /* strict */
        KtBlockExpression::class.java, KtParameterList::class.java, KtTypeParameterList::class.java /* Stop at */
    )?.let { return it }
    // try to handle an assignment case like `val foo: String; foo = "fun"`
    val binaryExpression = PsiTreeUtil.getParentOfType(
        host,
        KtBinaryExpression::class.java,
        false, /* strict */
        KtBlockExpression::class.java, KtParameterList::class.java, KtTypeParameterList::class.java /* Stop at */
    )?.takeIf { it.operationToken in KtTokens.ALL_ASSIGNMENTS } ?: return null
    val left = binaryExpression.left ?: return null
    return left.mainReference?.resolve() as? KtModifierListOwner
}

private fun findElementToInjectWithComment(host: KtElement): KtExpression? {
    val parentBlockExpression = PsiTreeUtil.getParentOfType(
        host,
        KtBlockExpression::class.java,
        true, /* strict */
        KtDeclaration::class.java /* Stop at */
    ) ?: return null

    return parentBlockExpression.statements.firstOrNull { statement ->
        PsiTreeUtil.isAncestor(statement, host, false) && checkIsValidPlaceForInjectionWithLineComment(statement, host)
    }
}

// Inspired with InjectorUtils.findCommentInjection()
private fun checkIsValidPlaceForInjectionWithLineComment(statement: KtExpression, host: KtElement): Boolean {
    // make sure comment is close enough and ...
    val statementStartOffset = statement.startOffset
    val hostStart = host.startOffset
    if (hostStart < statementStartOffset || hostStart - statementStartOffset > 120) {
        return false
    }

    if (hostStart - statementStartOffset > 2) {
        // ... there's no non-empty valid host in between comment and e2
        if (prevWalker(host, statement).asSequence().takeWhile { it != null }.any {
                it is PsiLanguageInjectionHost && it.isValidHost && !StringUtil.isEmptyOrSpaces(it.text)
            }
        ) return false
    }

    return true
}

private fun PsiElement.firstNonCommentChild(): PsiElement? =
    firstChild.siblings().dropWhile { it is PsiComment || it is PsiWhiteSpace }.firstOrNull()

// Based on InjectorUtils.prevWalker
private fun prevWalker(element: PsiElement, scope: PsiElement): Iterator<PsiElement?> {
    return object : Iterator<PsiElement?> {
        private var e: PsiElement? = element

        override fun hasNext(): Boolean = true
        override fun next(): PsiElement? {
            val current = e

            if (current == null || current === scope) return null
            val prev = current.prevSibling
            e = if (prev != null) {
                getDeepestLast(prev)
            } else {
                val parent = current.parent
                if (parent === scope || parent is PsiFile) null else parent
            }
            return e
        }
    }
}
