// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package com.intellij.codeInsight.completion;

import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.TailTypes;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupItem;
import com.intellij.codeInsight.lookup.PresentableLookupValue;
import com.intellij.diagnostic.PluginException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.patterns.CharPattern;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.ObjectPattern;
import com.intellij.psi.*;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.TrueFilter;
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static com.intellij.patterns.StandardPatterns.not;

/**
 * @deprecated see {@link CompletionContributor}
 */
@Deprecated(forRemoval = true)
public class CompletionData {
  private static final Logger LOG = Logger.getInstance(CompletionData.class);
  public static final ObjectPattern.Capture<Character> NOT_JAVA_ID = not(CharPattern.javaIdentifierPartCharacter());
  public static final Key<PsiReference> LOOKUP_ELEMENT_PSI_REFERENCE = Key.create("lookup element psi reference");
  private final List<CompletionVariant> myCompletionVariants = new ArrayList<>();

  protected CompletionData(){ }

  private boolean isScopeAcceptable(PsiElement scope){

    for (final CompletionVariant variant : myCompletionVariants) {
      if (variant.isScopeAcceptable(scope)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @deprecated see {@link CompletionContributor}
   */
  @Deprecated(forRemoval = true)
  protected void registerVariant(CompletionVariant variant){
    myCompletionVariants.add(variant);
  }

  public void completeReference(final PsiReference reference, final Set<? super LookupElement> set, final @NotNull PsiElement position, final PsiFile file) {
    final CompletionVariant[] variants = findVariants(position, file);
    boolean hasApplicableVariants = false;
    for (CompletionVariant variant : variants) {
      if (variant.hasReferenceFilter()) {
        variant.addReferenceCompletions(reference, position, set, file, this);
        hasApplicableVariants = true;
      }
    }

    if (!hasApplicableVariants) {
      myGenericVariant.addReferenceCompletions(reference, position, set, file, this);
    }
  }

  public void addKeywordVariants(Set<? super CompletionVariant> set, PsiElement position, final PsiFile file) {
    ContainerUtil.addAll(set, findVariants(position, file));
  }

  void completeKeywordsBySet(final Set<LookupElement> set, Set<? extends CompletionVariant> variants){
    for (final CompletionVariant variant : variants) {
      variant.addKeywords(set, this);
    }
  }

  public String findPrefix(PsiElement insertedElement, int offsetInFile){
    return findPrefixStatic(insertedElement, offsetInFile);
  }

  public CompletionVariant[] findVariants(final PsiElement position, final PsiFile file){
    final List<CompletionVariant> variants = new ArrayList<>();
      PsiElement scope = position;
      if(scope == null){
        scope = file;
      }
      while (scope != null) {
        boolean breakFlag = false;
        if (isScopeAcceptable(scope)){

          for (final CompletionVariant variant : myCompletionVariants) {
            if (variant.isVariantApplicable(position, scope) && !variants.contains(variant)) {
              variants.add(variant);
              if (variant.isScopeFinal(scope)) {
                breakFlag = true;
              }
            }
          }
        }
        if(breakFlag)
          break;
        scope = scope.getContext();
        if (scope instanceof PsiDirectory) break;
      }
      return variants.toArray(new CompletionVariant[0]);
  }

  protected final CompletionVariant myGenericVariant = new CompletionVariant() {
    @Override
    void addReferenceCompletions(PsiReference reference, PsiElement position, Set<? super LookupElement> set, final PsiFile file,
                                 final CompletionData completionData) {
      completeReference(reference, position, set, TailTypes.noneType(), TrueFilter.INSTANCE, this);
    }
  };

  private static String findPrefixStatic(final PsiElement insertedElement, final int offsetInFile, ElementPattern<Character> prefixStartTrim) {
    if(insertedElement == null) return "";

    final Document document = insertedElement.getContainingFile().getViewProvider().getDocument();
    assert document != null;
    LOG.assertTrue(!PsiDocumentManager.getInstance(insertedElement.getProject()).isUncommited(document), "Uncommitted");

    final String prefix = CompletionUtil.findReferencePrefix(insertedElement, offsetInFile);
    if (prefix != null) return prefix;

    if (insertedElement.getTextRange().equals(insertedElement.getContainingFile().getTextRange()) || insertedElement instanceof PsiComment) {
      return CompletionUtil.findJavaIdentifierPrefix(insertedElement, offsetInFile);
    }

    return findPrefixDefault(insertedElement, offsetInFile, prefixStartTrim);
  }

  /**
   * @deprecated Use {@link CompletionUtil} methods instead
   */
  @Deprecated(forRemoval = true)
  public static String findPrefixStatic(final PsiElement insertedElement, final int offsetInFile) {
    return findPrefixStatic(insertedElement, offsetInFile, NOT_JAVA_ID);
  }

  private static String findPrefixDefault(final PsiElement insertedElement, final int offset, final @NotNull ElementPattern trimStart) {
    String substr = insertedElement.getText().substring(0, offset - insertedElement.getTextRange().getStartOffset());
    if (substr.isEmpty() || Character.isWhitespace(substr.charAt(substr.length() - 1))) return "";

    substr = substr.trim();

    int i = 0;
    while (substr.length() > i && trimStart.accepts(substr.charAt(i))) i++;
    return substr.substring(i).trim();
  }

  public static @NotNull LookupElement objectToLookupItem(final @NotNull Object object) {
    if (object instanceof LookupElement lookupElement) return lookupElement;

    String s = null;
    TailType tailType = TailTypes.noneType();
    if (object instanceof PsiElement psiElement){
      s = PsiUtilCore.getName(psiElement);
    }
    else if (object instanceof PsiMetaData metaData) {
      s = metaData.getName();
    }
    else if (object instanceof String string) {
      s = string;
    }
    else if (object instanceof PresentableLookupValue lookupValue) {
      s = lookupValue.getPresentation();
    }
    if (s == null) {
      throw PluginException.createByClass("Null string for object: " + object + " of " + object.getClass(), null, object.getClass());
    }

    LookupItem<?> item = new LookupItem<>(object, s);

    item.setAttribute(LookupItem.TAIL_TYPE_ATTR, tailType);
    return item;
  }


  protected void addLookupItem(Set<? super LookupElement> set, TailType tailType, @NotNull Object completion, final CompletionVariant variant) {
    LookupElement ret = objectToLookupItem(completion);

    if (!(ret instanceof LookupItem item)) {
      set.add(ret);
      return;
    }

    InsertHandler<?> insertHandler = variant.getInsertHandler();
    if(insertHandler != null && item.getInsertHandler() == null) {
      item.setInsertHandler(insertHandler);
      item.setTailType(TailTypes.unknownType());
    }
    else if (tailType != TailTypes.noneType()) {
      item.setTailType(tailType);
    }
    final Map<Object, Object> itemProperties = variant.getItemProperties();
    for (final Object key : itemProperties.keySet()) {
      item.setAttribute(key, itemProperties.get(key));
    }

    set.add(ret);
  }

  protected void completeReference(PsiReference reference, PsiElement position, Set<? super LookupElement> set, TailType tailType, ElementFilter filter, CompletionVariant variant) {
    if (reference instanceof PsiMultiReference multiReference) {
      for (PsiReference ref : getReferences(multiReference)) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else if (reference instanceof PsiReferencesWrapper wrapper) {
      for (PsiReference ref : wrapper.getReferences()) {
        completeReference(ref, position, set, tailType, filter, variant);
      }
    }
    else{
      final Object[] completions = reference.getVariants();
      putSourceInformation(reference, completions);
      for (Object completion : completions) {
        if (completion == null) {
          LOG.error("Position=" + position + "\n;Reference=" + reference + "\n;variants=" + Arrays.toString(completions));
          continue;
        }
        if (completion instanceof PsiElement psiElement) {
          if (filter.isClassAcceptable(psiElement.getClass()) && filter.isAcceptable(psiElement, position)) {
            addLookupItem(set, tailType, completion, variant);
          }
        }
        else {
          if (completion instanceof LookupItem<?> lookupItem) {
            final Object o = lookupItem.getObject();
            if (o instanceof PsiElement) {
              if (!filter.isClassAcceptable(o.getClass()) || !filter.isAcceptable(o, position)) continue;
            }
          }
          try {
            addLookupItem(set, tailType, completion, variant);
          }
          catch (AssertionError e) {
            LOG.error("Caused by variant from reference: " + reference.getClass(), e);
          }
        }
      }
    }
  }

  private static void putSourceInformation(@NotNull PsiReference reference, Object[] referencesAsObjects) {
    for (Object referenceAsObject : referencesAsObjects) {
      if (referenceAsObject instanceof UserDataHolderBase userDataHolderBaseReference) {
        userDataHolderBaseReference.putUserData(LOOKUP_ELEMENT_PSI_REFERENCE, reference);
      }
    }
  }

  protected static PsiReference[] getReferences(final PsiMultiReference multiReference) {
    final PsiReference[] references = multiReference.getReferences();
    final List<PsiReference> hard = ContainerUtil.findAll(references, object -> !object.isSoft());
    if (!hard.isEmpty()) {
      return hard.toArray(PsiReference.EMPTY_ARRAY);
    }
    return references;
  }

  void addKeywords(Set<LookupElement> set, CompletionVariant variant, Object comp, TailType tailType) {
    if (!(comp instanceof String)) return;

    for (final LookupElement item : set) {
      if (item.getObject().toString().equals(comp)) {
        return;
      }
    }
    addLookupItem(set, tailType, comp, variant);
  }
}
