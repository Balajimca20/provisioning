package com.royalenfield.ffmechanic.app.feature.wifi.domain;

import com.royalenfield.ffmechanic.app.core.adb.AdbManager;
import com.royalenfield.ffmechanic.app.feature.wifi.data.WifiChangeLogRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class WifiUpdateWorkflow_Factory implements Factory<WifiUpdateWorkflow> {
  private final Provider<AdbManager> adbManagerProvider;

  private final Provider<WifiChangeLogRepository> wifiChangeLogProvider;

  public WifiUpdateWorkflow_Factory(Provider<AdbManager> adbManagerProvider,
      Provider<WifiChangeLogRepository> wifiChangeLogProvider) {
    this.adbManagerProvider = adbManagerProvider;
    this.wifiChangeLogProvider = wifiChangeLogProvider;
  }

  @Override
  public WifiUpdateWorkflow get() {
    return newInstance(adbManagerProvider.get(), wifiChangeLogProvider.get());
  }

  public static WifiUpdateWorkflow_Factory create(Provider<AdbManager> adbManagerProvider,
      Provider<WifiChangeLogRepository> wifiChangeLogProvider) {
    return new WifiUpdateWorkflow_Factory(adbManagerProvider, wifiChangeLogProvider);
  }

  public static WifiUpdateWorkflow newInstance(AdbManager adbManager,
      WifiChangeLogRepository wifiChangeLog) {
    return new WifiUpdateWorkflow(adbManager, wifiChangeLog);
  }
}
