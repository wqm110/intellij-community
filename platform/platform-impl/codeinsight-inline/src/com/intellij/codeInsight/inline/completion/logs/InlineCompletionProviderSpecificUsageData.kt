// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.inline.completion.logs

import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.ExtensionPointName.Companion.create
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus

/**
 * Allows extending information in fus logs about inline completion by provider-specific usage data.
 */
@ApiStatus.Internal
interface InlineCompletionProviderSpecificUsageData {
  class InvocationDescriptor(val editor: Editor, val file: PsiFile?)
  /**
   * Use this function carefully.
   * If you write information on every typing and cancellation, it slows down the editor and typing.
   */
  fun getAdditionalInvocationUsageData(descriptor: InvocationDescriptor): List<EventPair<*>> = emptyList()

  /**
   * Collecting usage data potentially can be used not only for logging,
   * so in case you want to collect data, but not log it, override this function.
   */
  fun skipLogging() = false

  /**
   * Can be useful for getting specific usage data using:
   *    EP_NAME.findByIdOrFromInstance("some_id") { it.getId() }
   */
  fun getId(): String

  companion object {
    val EP_NAME: ExtensionPointName<InlineCompletionProviderSpecificUsageData> = create("com.intellij.inline.completion.usage.data")
  }

  //<extensionPoint name="lookup.usageDetails" interface="com.intellij.codeInsight.lookup.impl.LookupUsageDescriptor" dynamic="true"/>
}
