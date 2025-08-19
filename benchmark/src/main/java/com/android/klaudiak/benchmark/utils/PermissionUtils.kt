package com.android.klaudiak.benchmark.utils

import android.Manifest
import android.content.res.Resources
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert
import java.util.regex.Pattern

fun MacrobenchmarkScope.acceptPermissionInSystemLanguage() {
    val locale = Resources.getSystem().configuration.locales[0]
    Log.d("STTBenchmark", "Testing with system language: ${locale.language}-${locale.country}")

    when (locale.language) {
        "pl" -> acceptPermission(listOf("Podczas używania aplikacji", "Zezwól", "Pozwól"))
        else -> acceptPermission(listOf("While using the app", "Allow", "OK"))
    }
}

fun MacrobenchmarkScope.acceptPermission(acceptTexts: List<String>) {
    Log.d("STTBenchmark", "Accepting permission dialog with possible texts: $acceptTexts")

    for (acceptText in acceptTexts) {
        try {
            val selector = By.text(Pattern.compile(acceptText, Pattern.CASE_INSENSITIVE))
            if (device.wait(Until.hasObject(selector), 5000)) {
                val allowPermission = device.findObject(UiSelector().textMatches("(?i)$acceptText"))
                if (allowPermission.exists()) {
                    allowPermission.click()
                    Log.d("STTBenchmark", "Clicked permission button with text: $acceptText")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w("STTBenchmark", "Failed to find or click permission with text: $acceptText", e)
        }
    }

    try {
        val buttonIds = listOf(
            "com.android.permissioncontroller:id/permission_allow_button",
            "com.android.packageinstaller:id/permission_allow_button",
            "android:id/button1"
        )

        for (buttonId in buttonIds) {
            val allowButton = device.findObject(UiSelector().resourceId(buttonId))
            if (allowButton.exists()) {
                allowButton.click()
                Log.d("STTBenchmark", "Clicked permission button with resource ID: $buttonId")
                return
            }
        }
    } catch (e: Exception) {
        Log.e("STTBenchmark", "Failed to find or click permission button by resource ID", e)
    }

    Log.e("STTBenchmark", "Could not find any permission dialog to accept")
}

private fun isPolishSystemLanguage(device: UiDevice): Boolean {
    return try {
        val locale = device.executeShellCommand("getprop persist.sys.locale").trim().lowercase()
        locale.contains("pl") || locale.contains("pol")
    } catch (e: Exception) {
        Log.e("STTBenchmark", "Error detecting system language", e)
        false
    }
}

fun MacrobenchmarkScope.acceptPermissionInPolish() {
    device.wait(Until.hasObject(By.text("Podczas używania aplikacji")), 15000)
    val allowPermission: UiObject =
        device.findObject(UiSelector().text("Podczas używania aplikacji"))

    if (allowPermission.exists()) {
        try {
            allowPermission.click()
        } catch (e: UiObjectNotFoundException) {
            Log.e(e.cause.toString(), "There is no permission dialog")
        }
    }
}

fun MacrobenchmarkScope.acceptPermissionInEnglish() {
    device.wait(Until.hasObject(By.text("Allow")), 15000)
    val allowPermission: UiObject =
        device.findObject(UiSelector().text("Allow"))

    if (allowPermission.exists()) {
        try {
            allowPermission.click()
        } catch (e: UiObjectNotFoundException) {
            Log.e(e.cause.toString(), "There is no permission dialog")
        }
    }
}

fun MacrobenchmarkScope.resetPermission() {
    val command = "cmd appops reset com.android.klaudiak.sttlab"
    val output = device.executeShellCommand(command)
    Assert.assertEquals("", output)
}

fun MacrobenchmarkScope.allowRecordAudio() {
    val command = "pm grant com.android.klaudiak.sttlab ${Manifest.permission.RECORD_AUDIO}"
    val output = device.executeShellCommand(command)
    Assert.assertEquals("", output)
}

