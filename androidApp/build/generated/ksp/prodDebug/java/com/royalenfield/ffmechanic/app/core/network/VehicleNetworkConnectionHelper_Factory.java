package com.royalenfield.ffmechanic.app.core.network;

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
public final class VehicleNetworkConnectionHelper_Factory implements Factory<VehicleNetworkConnectionHelper> {
  private final Provider<Context> contextProvider;

  public VehicleNetworkConnectionHelper_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public VehicleNetworkConnectionHelper get() {
    return newInstance(contextProvider.get());
  }

  public static VehicleNetworkConnectionHelper_Factory create(Provider<Context> contextProvider) {
    return new VehicleNetworkConnectionHelper_Factory(contextProvider);
  }

  public static VehicleNetworkConnectionHelper newInstance(Context context) {
    return new VehicleNetworkConnectionHelper(context);
  }
}
