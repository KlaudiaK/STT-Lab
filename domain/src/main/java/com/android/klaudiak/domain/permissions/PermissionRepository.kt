package com.android.klaudiak.domain.permissions

interface PermissionRepository {
    fun hasRecordAudioPermission(): Boolean
}