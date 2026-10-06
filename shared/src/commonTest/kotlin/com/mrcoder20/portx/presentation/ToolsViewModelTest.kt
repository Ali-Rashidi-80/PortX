package com.mrcoder20.portx.presentation

import com.mrcoder20.portx.presentation.viewmodel.ToolsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ToolsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ToolsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ToolsViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialToolState() {
        assertEquals("LOCAL", viewModel.uiState.value.activeTool)
        assertEquals("", viewModel.uiState.value.target)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testTargetChange() {
        viewModel.onTargetChange("192.168.1.254")
        assertEquals("192.168.1.254", viewModel.uiState.value.target)
        assertNull(viewModel.uiState.value.error)
    }

    @Test
    fun testSelectToolResetsOutputs() {
        viewModel.selectTool("PING")
        assertEquals("PING", viewModel.uiState.value.activeTool)
        assertEquals(emptyList(), viewModel.uiState.value.pingResults)

        viewModel.selectTool("DNS")
        assertEquals("DNS", viewModel.uiState.value.activeTool)
        assertEquals(emptyList(), viewModel.uiState.value.dnsResults)

        viewModel.selectTool("WHOIS")
        assertEquals("WHOIS", viewModel.uiState.value.activeTool)
        assertNull(viewModel.uiState.value.whoisResult)
    }

    @Test
    fun testStopActiveTool() {
        viewModel.stopActiveTool()
        assertEquals(false, viewModel.uiState.value.isLoading)
    }
}
