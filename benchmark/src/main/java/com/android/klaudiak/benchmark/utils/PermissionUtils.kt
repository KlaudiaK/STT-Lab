package com.android.klaudiak.benchmark.utils

import android.Manifest
import android.util.Log
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import org.junit.Assert

fun MacrobenchmarkScope.acceptPermission() {
    device.wait(Until.hasObject(By.text("Allow")), 15000)
    val allowPermission: UiObject = device.findObject(UiSelector().text("Allow"))

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

