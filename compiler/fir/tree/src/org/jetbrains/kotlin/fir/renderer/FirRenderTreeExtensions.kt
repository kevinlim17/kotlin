/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElement
import java.io.StringWriter
import java.io.Writer

fun FirRenderer.renderElementAsTree(element: FirElement, writer: Writer): Writer {
    val visitor = FirTreeDumpVisitor(writer)
    element.accept(visitor)

    return writer
}

fun FirRenderer.renderElementAsTreeString(element: FirElement): String {
    val stringWriter = StringWriter()
    renderElementAsTree(element, stringWriter)
    return stringWriter.toString()
}