/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.renderer.FirRenderer
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.body.resolve.FirImplicitTypeBodyResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.contracts.FirContractResolveProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.mpp.FirExpectActualMatcherProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirAnnotationArgumentsProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompanionGenerationProcessor
import org.jetbrains.kotlin.fir.resolve.transformers.plugin.FirCompilerRequiredAnnotationsResolveProcessor

/** Enable Generating Dump? */
data class DumpConfig(
    val enabled: Boolean = true,
    val phaseFilter: Set<String> = FirResolvePhase.entries.map { it.toString().lowercase() }.toSet()
)

object DumpFirTask {
    private val scheduledDumpList = mutableListOf<Pair<String, FirFile>>()

    private var _config: DumpConfig = DumpConfig()
    val config: DumpConfig get() = _config

    /** Should compiler dump the phase? */
    fun shouldDumpPhase(phase: FirResolvePhase?): Boolean {
        if (phase == null || !config.enabled) return false
        val phaseName = phase.toString().lowercase()
        return config.phaseFilter.isEmpty() || phaseName in config.phaseFilter
    }

    /** Define phase from processor object*/
    fun getPhaseFromProcessor(processor: FirResolveProcessor): FirResolvePhase? {
        return when (processor) {
            is FirImportResolveProcessor -> FirResolvePhase.IMPORTS
            is FirSupertypeResolverProcessor -> FirResolvePhase.SUPER_TYPES
            is FirSealedClassInheritorsProcessor -> FirResolvePhase.SEALED_CLASS_INHERITORS
            is FirTypeResolveProcessor -> FirResolvePhase.TYPES
            is FirStatusResolveProcessor -> FirResolvePhase.STATUS
            is FirContractResolveProcessor -> FirResolvePhase.CONTRACTS
            is FirImplicitTypeBodyResolveProcessor -> FirResolvePhase.IMPLICIT_TYPES_BODY_RESOLVE
            is FirConstantEvaluationProcessor -> FirResolvePhase.CONSTANT_EVALUATION
            is FirAnnotationArgumentsProcessor -> FirResolvePhase.ANNOTATION_ARGUMENTS
            is FirBodyResolveProcessor -> FirResolvePhase.BODY_RESOLVE
            is FirExpectActualMatcherProcessor -> FirResolvePhase.EXPECT_ACTUAL_MATCHING
            is FirCompilerRequiredAnnotationsResolveProcessor -> FirResolvePhase.COMPILER_REQUIRED_ANNOTATIONS
            is FirCompanionGenerationProcessor -> FirResolvePhase.COMPANION_GENERATION
            else -> null
        }
    }

    /** Add dump data to queue */
    private fun enqueue(phase: FirResolvePhase?, input: FirFile) {
        if (phase != null && shouldDumpPhase(phase)) {
            scheduledDumpList.add(phase.toString() to input)
        }
    }

    fun enqueueFromProcessor(processor: FirResolveProcessor, input: FirFile) {
        val phase = getPhaseFromProcessor(processor)
        enqueue(phase, input)
    }

    fun flush() {
        val firElementVisitor = FirRenderer()

        for ((phaseName, input) in scheduledDumpList) {
            /**
            File("build/fir-phase-dumps").apply { mkdirs() }
            .resolve("${phaseName.lowercase()}-fir.txt")
            .also { it.writeText(input.toString()) }
            .also { println("[org.jetbrains.kotlin.backend.common.phaser.PhaseEngine] Dumped FIR after phase: $phaseName to ${it.absolutePath}") }
             */
            println("=== [FIR DUMP] Phase: $phaseName ===")
            firElementVisitor.renderElementAsString(input)
            println("=== [END OF FIR DUMP: $phaseName] === \n")
        }
        scheduledDumpList.clear()
    }
}