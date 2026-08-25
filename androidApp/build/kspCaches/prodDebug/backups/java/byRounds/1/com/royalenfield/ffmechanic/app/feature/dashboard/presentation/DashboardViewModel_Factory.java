package com.royalenfield.ffmechanic.app.feature.dashboard.presentation;

import android.content.Context;
import com.royalenfield.ffmechanic.app.core.adb.AdbClient;
import com.royalenfield.ffmechanic.app.core.network.VehicleNetworkConnectionHelper;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AdbClient> adbClientProvider;

  private final Provider<VehicleNetworkConnectionHelper> networkHelperProvider;

  private final Provider<Context> contextProvider;

  public DashboardViewModel_Factory(Provider<AdbClient> adbClientProvider,
      Provider<VehicleNetworkConnectionHelper> networkHelperProvider,
      Provider<Context> contextProvider) {
    this.adbClientProvider = adbClientProvider;
    this.networkHelperProvider = networkHelperProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(adbClientProvider.get(), networkHelperProvider.get(), contextProvider.get());
  }

  public static DashboardViewModel_Factory create(Provider<AdbClient> adbClientProvider,
      Provider<VehicleNetworkConnectionHelper> networkHelperProvider,
      Provider<Context> contextProvider) {
    return new DashboardViewModel_Factory(adbClientProvider, networkHelperProvider, contextProvider);
  }

  public static DashboardViewModel newInstance(AdbClient adbClient,
      VehicleNetworkConnectionHelper networkHelper, Context context) {
    return new DashboardViewModel(adbClient, networkHelper, context);
  }
}
