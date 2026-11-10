package com.newoether.agora.viewmodel

/** Launches independent, one-shot maintenance after the conversation list is visible. */
internal class StartupMaintenanceCoordinator(
    private val startAutoBackup: () -> Unit,
    private val startSemanticIndex: () -> Unit,
) {
    fun start() {
        startAutoBackup()
        startSemanticIndex()
    }
}
