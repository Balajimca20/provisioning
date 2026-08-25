package com.royalenfield.ffmechanic.app.core.di

import com.royalenfield.ffmechanic.app.core.adb.AdbClient
import com.royalenfield.ffmechanic.app.core.adb.AdbKeyPairProvider
import com.royalenfield.ffmechanic.app.core.network.GraphQLClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGraphQLClient(): GraphQLClient = GraphQLClient()

    @Provides
    @Singleton
    fun provideAdbClient(keyPairProvider: AdbKeyPairProvider): AdbClient = AdbClient(keyPairProvider)
}
