package com.android.klaudiak.domain.modules

import android.content.Context
import com.android.klaudiak.domain.permissions.PermissionRepository
import com.android.klaudiak.domain.permissions.PermissionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PermissionModule {

    @Provides
    @Singleton
    fun providePermissionRepository(@ApplicationContext context: Context): PermissionRepository {
        return PermissionRepositoryImpl(context)
    }
}