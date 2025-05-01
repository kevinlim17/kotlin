/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.renderer

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.FirVariable
import org.jetbrains.kotlin.fir.declarations.utils.modality
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.arguments
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import java.io.Writer

data class SafeIndent(
    val value: Int = 0,
)


class FirTreeDumpVisitor(
    private val writer: Writer,
    private val indentation: String = " ",
) : FirVisitorVoid() {

    private var _safeIndent = SafeIndent(0)
    val safeIndent: SafeIndent get() = _safeIndent

    private fun writeIndent() = try {
        repeat(safeIndent.value) {
            writer.write(indentation)
        }
    } catch (e: Exception) {
        TODO("적절한 Exception Logic 구현")
    }

    private fun writeLine(text: String) {
        try {
            writeIndent()
            writer.write(text)
            writer.write("\n")
        } catch (e: Exception) {
            TODO("적절한 Exception Logic 구현")
        }
    }

    private fun setNewIndentValue(inc: Int) {
        val newSafeIndentValue = safeIndent.value.plus(inc)
        _safeIndent = SafeIndent(newSafeIndentValue)
    }

    private inline fun withIndent(f: () -> Unit) {
        setNewIndentValue(1)
        f()
        setNewIndentValue(-1)
    }

    override fun visitElement(element: FirElement) {
        writeLine("${element.javaClass.simpleName}")
        withIndent {
            visitChildren(element)
        }
    }

    override fun visitFile(file: FirFile) {
        writeLine(file.name)
        withIndent { visitChildren(file) }
    }

    override fun visitRegularClass(regularClass: FirRegularClass) {
        val visibility = regularClass.visibility.toString()
        val modality = regularClass.modality.toString()
        writeLine("Class: ${regularClass.name} (${regularClass.classKind}) [$visibility $modality]")
        withIndent {
            visitChildren(regularClass)
        }
    }

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
        val visibility = simpleFunction.visibility.toString()
        val modality = simpleFunction.modality.toString()
        writeLine("Function: ${simpleFunction.name} [$visibility $modality]")
        withIndent {
            visitChildren(simpleFunction)
        }
    }

    override fun visitProperty(property: FirProperty) {
        val visibility = property.visibility.toString()
        val modality = property.modality.toString()
        writeLine("Property: ${property.name} [$visibility $modality]")
        withIndent {
            visitChildren(property)
        }
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef) {
        writeLine("Resolved Type : ${resolvedTypeRef.coneType}")
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall) {
        writeLine("Function Call: ${functionCall.calleeReference}")
        withIndent {
            writeLine("Arguments: ")
            withIndent {
                functionCall.arguments.forEach { it.accept(this) }
            }
        }
    }

    private fun visitChildren(element: FirElement) {
        when (element) {
            is FirFile -> {
                writeLine("Package: ${element.packageDirective.packageFqName}")
                if (element.imports.isNotEmpty()) {
                    writeLine("Imports: ")
                    withIndent {
                        element.imports.forEach { it.accept(this) }
                    }
                }
                if (element.declarations.isNotEmpty()) {
                    writeLine("Declarations: ")
                    withIndent {
                        element.declarations.forEach { it.accept(this) }
                    }
                }
            }
            is FirClass -> {
                if (element.typeParameters.isNotEmpty()) {
                    writeLine("Type Parameters: ")
                    withIndent {
                        element.typeParameters.forEach { it.accept(this) }
                    }
                }
                if (element.superTypeRefs.isNotEmpty()) {
                    writeLine("Super Types: ")
                    withIndent {
                        element.superTypeRefs.forEach { it.accept(this) }
                    }
                }
                if (element.declarations.isNotEmpty()) {
                    writeLine("Declarations: ")
                    withIndent {
                        element.declarations.forEach { it.accept(this) }
                    }
                }
            }
            is FirFunction -> {
                element.receiverParameter?.let {
                    writeLine("Receiver: ")
                    withIndent { it.accept(this) }
                }
                if (element.valueParameters.isNotEmpty()) {
                    writeLine("Parameters: ")
                }
                element.returnTypeRef.let {
                    writeLine("Return Type:")
                    withIndent { it.accept(this) }
                }
                element.body?.let {
                    writeLine("Body: ")
                    withIndent { it.accept(this) }
                }
            }
            is FirBlock -> {
                element.statements.forEach { it.accept(this) }
            }
            is FirVariable -> {
                element.initializer?.accept(this)
            }
        }
    }


}