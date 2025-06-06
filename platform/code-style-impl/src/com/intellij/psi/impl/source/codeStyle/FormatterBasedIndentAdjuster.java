// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.psi.impl.source.codeStyle;

import com.intellij.formatting.FormattingMode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.text.BlockSupport;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
final class FormatterBasedIndentAdjuster  {
  private static final int MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE = 100000;

  private FormatterBasedIndentAdjuster() {
  }

  static void scheduleIndentAdjustment(@NotNull Project project,
                                       @NotNull Document document,
                                       int offset) {
    PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(document);
    if (file != null) {
      IndentAdjusterRunnable fixer = new IndentAdjusterRunnable(project, document, file, offset);
      PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
      if (isSynchronousAdjustment(document, file)) {
        documentManager.commitDocument(document);
      }
      fixer.run();
    }
  }

  private static boolean isSynchronousAdjustment(@NotNull Document document, @NotNull PsiFile file) {
    return ApplicationManager.getApplication().isUnitTestMode() ||
           document.getTextLength() <= MAX_SYNCHRONOUS_ADJUSTMENT_DOC_SIZE && !BlockSupport.isTooDeep(file);
  }

  static final class IndentAdjusterRunnable implements Runnable {
    private final Project myProject;
    private final int myLine;
    private final Document myDocument;
    private final PsiFile myPsiFile;

    IndentAdjusterRunnable(@NotNull Project project,
                           @NotNull Document document,
                           @NotNull PsiFile psiFile,
                           int offset) {
      myProject = project;
      myDocument = document;
      myLine = myDocument.getLineNumber(offset);
      myPsiFile = psiFile;
    }

    @Override
    public void run() {
      int lineStart = myDocument.getLineStartOffset(myLine);
      int indentEnd = CharArrayUtil.shiftForward(myDocument.getCharsSequence(), lineStart, " \t");
      RangeMarker indentMarker = myDocument.createRangeMarker(lineStart, indentEnd);
      CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(myProject);
      if (isSynchronousAdjustment(myDocument, myPsiFile)) {
        updateIndent(indentMarker,
                     codeStyleManager.getLineIndent(myPsiFile, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER));
      }
      else {
        ReadAction
          .nonBlocking(() -> codeStyleManager.getLineIndent(myPsiFile, lineStart, FormattingMode.ADJUST_INDENT_ON_ENTER))
          .withDocumentsCommitted(myProject)
          .finishOnUiThread(ModalityState.nonModal(), indentString -> updateIndent(indentMarker, indentString))
          .submit(AppExecutorUtil.getAppExecutorService());
      }
    }

    private void updateIndent(@NotNull RangeMarker indentMarker, @Nullable String newIndent) {
      if (newIndent != null) {
        CommandProcessor.getInstance().runUndoTransparentAction(
          () ->
            ApplicationManager.getApplication().runWriteAction(() -> {
              myDocument.replaceString(indentMarker.getStartOffset(), indentMarker.getEndOffset(), newIndent);
              indentMarker.dispose();
            }));
      }
    }
  }
}
