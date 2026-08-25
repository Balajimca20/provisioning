package com.royalenfield.ffmechanic.app.feature.wifi.data;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class WifiChangeLogRepository_Factory implements Factory<WifiChangeLogRepository> {
  private final Provider<Context> contextProvider;

  public WifiChangeLogRepository_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public WifiChangeLogRepository get() {
    return newInstance(contextProvider.get());
  }

  public static WifiChangeLogRepository_Factory create(Provider<Context> contextProvider) {
    return new WifiChangeLogRepository_Factory(contextProvider);
  }

  public static WifiChangeLogRepository newInstance(Context context) {
    return new WifiChangeLogRepository(context);
  }
}
