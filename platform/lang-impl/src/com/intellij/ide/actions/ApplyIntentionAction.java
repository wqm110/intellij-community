// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.ide.actions;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionSource;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.NlsActions;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class ApplyIntentionAction extends AnAction {

  private final IntentionAction myAction;
  private final Editor myEditor;
  private final PsiFile myPsiFile;

  public ApplyIntentionAction(final HighlightInfo.IntentionActionDescriptor descriptor, @NlsActions.ActionText String text, Editor editor, PsiFile psiFile) {
    this(descriptor.getAction(), text, editor, psiFile);
  }

  public ApplyIntentionAction(final IntentionAction action, @NlsActions.ActionText String text, Editor editor, PsiFile psiFile) {
    super("");
    getTemplatePresentation().setText(text, false);
    myAction = action;
    myEditor = editor;
    myPsiFile = psiFile;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiDocumentManager.getInstance(myPsiFile.getProject()).commitAllDocuments();
    ShowIntentionActionsHandler.chooseActionAndInvoke(myPsiFile, myEditor, myAction, myAction.getText(), IntentionSource.SEARCH_EVERYWHERE);
  }

  public String getName() {
    return ReadAction.compute(() -> myAction.getText());
  }

  public static ApplyIntentionAction @Nullable [] getAvailableIntentions(@NotNull Editor editor, @NotNull PsiFile file) {
    ShowIntentionsPass.IntentionsInfo info = ShowIntentionsPass.getActionsToShow(editor, file);
    if (info.isEmpty()) return null;

    final List<HighlightInfo.IntentionActionDescriptor> actions = new ArrayList<>();
    actions.addAll(info.errorFixesToShow);
    actions.addAll(info.inspectionFixesToShow);
    actions.addAll(info.intentionsToShow);

    final ApplyIntentionAction[] result = new ApplyIntentionAction[actions.size()];
    for (int i = 0; i < result.length; i++) {
      final HighlightInfo.IntentionActionDescriptor descriptor = actions.get(i);
      final String actionText = ReadAction.compute(() -> descriptor.getAction().getText());
      result[i] = new ApplyIntentionAction(descriptor, actionText, editor, file);
    }
    return result;
  }
}
