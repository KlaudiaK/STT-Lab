package com.android.klaudiak.benchmark

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MemoryUsageMetric
import androidx.benchmark.macro.PowerMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.android.klaudiak.benchmark.utils.acceptPermission
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CpuMemoryUsageBenchmark {

    @get:Rule
    val grantPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    @RequiresApi(Build.VERSION_CODES.Q)
    @OptIn(ExperimentalMetricApi::class)
    @Test
    fun measureSherpaNcnnActivity() {

//        val instrumentation = InstrumentationRegistry.getInstrumentation()
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            val uiAutomation = instrumentation.uiAutomation
//            uiAutomation.executeShellCommand(
//                "pm grant com.android.klaudiak.sttlab android.permission.RECORD_AUDIO"
//            ).close()
//        }

        val powerMetricSupported = try {
            PowerMetric::class.java.methods.any {
                it.name == "deviceSupportsPowerEnergy"
            }
        } catch (e: Exception) {
            false
        }

        val metrics = mutableListOf(
            StartupTimingMetric(),
            FrameTimingMetric(),
            MemoryUsageMetric(MemoryUsageMetric.Mode.Max),
           /* if (PowerMetric.deviceSupportsHighPrecisionTracking()) {
                PowerMetric(PowerMetric.Type.Energy())
            } else {
                PowerMetric(PowerMetric.Type.Battery())
            }*/
        )

        if (powerMetricSupported) {
            try {
                metrics.add(PowerMetric(PowerMetric.Type.Power()))
            } catch (e: UnsupportedOperationException) {
                Log.w("BenchmarkTest", "Power metrics not supported on this device")
            }
        }

        benchmarkRule.measureRepeated(
            packageName = "com.android.klaudiak.sttlab",
            metrics = metrics,
            compilationMode = CompilationMode.Partial(),
            iterations = 4,
            startupMode = StartupMode.WARM,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                acceptPermission()

            }
        ) {
            with (device) {
                wait(Until.hasObject(By.text("Start")), 5000)
                findObject(By.text("Start")).click()
                wait(Until.hasObject(By.text("Play from Beginning")), 5000)
                findObject(By.text("Play from Beginning")).click()

                wait(Until.hasObject(By.text("Stop")), 5000)
                val stopButton = findObject(By.text("Stop"))
                stopButton.click()
            }
        }
    }
}


