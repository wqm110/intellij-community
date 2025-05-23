// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.debugger.streams.core.diagnostic.ex;

import org.jetbrains.annotations.NotNull;

/**
 * @author Vitaliy.Bibaev
 */
public final class TraceEvaluationException extends TraceException {
  public TraceEvaluationException(@NotNull String message, @NotNull String traceExpression) {
    super(message, traceExpression);
  }
}
