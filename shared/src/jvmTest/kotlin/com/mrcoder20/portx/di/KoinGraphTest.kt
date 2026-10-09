package com.mrcoder20.portx.di

import com.mrcoder20.portx.domain.usecase.*
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class KoinGraphTest {

    @BeforeTest
    fun setup() {
        stopKoin()
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun testUsecaseFactoriesDefinition() {
        val testModule = module {
            factory { SecurityScoreUseCase() }
            factory { FirewallDetectionUseCase() }
            factory { AnomalyDetectionUseCase() }
            factory { DeviceFingerprintUseCase() }
            factory { ExportReportUseCase() }
        }

        val app = startKoin {
            modules(testModule)
        }

        assertNotNull(app.koin.get<SecurityScoreUseCase>())
        assertNotNull(app.koin.get<FirewallDetectionUseCase>())
        assertNotNull(app.koin.get<AnomalyDetectionUseCase>())
        assertNotNull(app.koin.get<DeviceFingerprintUseCase>())
        assertNotNull(app.koin.get<ExportReportUseCase>())
    }
}
