package com.newoether.agora.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupMaintenanceCoordinatorTest {
    @Test
    fun startsOnlyLocalMaintenanceTasks() {
        val events = mutableListOf<String>()
        val coordinator = StartupMaintenanceCoordinator(
            startAutoBackup = { events += "backup" },
            startSemanticIndex = { events += "semantic" },
        )

        coordinator.start()

        assertEquals(listOf("backup", "semantic"), events)
    }
}
