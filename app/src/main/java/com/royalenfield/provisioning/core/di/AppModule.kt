package com.royalenfield.provisioning.core.di

import com.royalenfield.provisioning.BuildConfig
import com.royalenfield.provisioning.core.adb.AdbClient
import com.royalenfield.provisioning.core.adb.AdbKeyPairProvider
import com.royalenfield.provisioning.core.adb.AdbManager
import com.royalenfield.provisioning.core.network.GraphQLClient
import com.royalenfield.provisioning.core.network.KtorClientFactory
import com.royalenfield.provisioning.core.network.VehicleNetworkConnectionHelper
import com.royalenfield.provisioning.feature.dashboard.presentation.DashboardViewModel
import com.royalenfield.provisioning.feature.ota.data.OtaRepository
import com.royalenfield.provisioning.feature.ota.domain.OtaPipeline
import com.royalenfield.provisioning.feature.ota.presentation.CommandLineOTAViewModel
import com.royalenfield.provisioning.feature.supplierfeed.data.SupplierFeedRepository
import com.royalenfield.provisioning.feature.supplierfeed.domain.FetchTelemetryUseCase
import com.royalenfield.provisioning.feature.supplierfeed.presentation.SupplierFeedViewModel
import com.royalenfield.provisioning.feature.terminal.presentation.TerminalViewModel
import com.royalenfield.provisioning.feature.wifitracker.data.WifiTrackerRepository
import com.royalenfield.provisioning.feature.wifitracker.domain.WifiUpdateWorkflow
import com.royalenfield.provisioning.feature.wifitracker.presentation.WifiTrackerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single {
        KtorClientFactory(
            apiKeyProvider = { BuildConfig.SUPPLIER_FEED_API_KEY }
        ).createClient()
    }

    single {
        GraphQLClient(
            httpClient = get(),
            baseUrlProvider = { BuildConfig.FF_BASE_URL.ifEmpty { "https://api.royalenfield.com" } }
        )
    }

    single { VehicleNetworkConnectionHelper(androidContext()) }
}

val adbModule = module {
    single { AdbKeyPairProvider(androidContext()) }
    single { AdbClient(get()) }
    single { AdbManager(androidContext(), get()) }
}

val repositoryModule = module {
    single { WifiTrackerRepository(androidContext()) }
    single { OtaRepository(httpClient = get(), adbClient = get()) }
    single { SupplierFeedRepository(graphQLClient = get(), httpClient = get()) }
}

val domainModule = module {
    single { WifiUpdateWorkflow(adbManager = get(), logRepository = get()) }
    single { OtaPipeline(otaRepository = get(), adbClient = get()) }
    single { FetchTelemetryUseCase(supplierFeedRepository = get()) }
}

val viewModelModule = module {
    viewModel {
        DashboardViewModel(
            networkHelper = get(),
            adbClient = get(),
            adbManager = get()
        )
    }

    viewModel {
        WifiTrackerViewModel(
            workflow = get(),
            logRepository = get(),
            adbManager = get()
        )
    }

    viewModel {
        CommandLineOTAViewModel(
            adbClient = get(),
            context = androidContext()
        )
    }

    viewModel {
        SupplierFeedViewModel(
            fetchTelemetryUseCase = get()
        )
    }

    viewModel {
        TerminalViewModel(
            adbClient = get()
        )
    }
}

val appModules = listOf(
    networkModule,
    adbModule,
    repositoryModule,
    domainModule,
    viewModelModule
)
