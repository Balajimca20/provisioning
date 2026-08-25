package com.royalenfield.ffmechanic.app.feature.ota.domain;

import com.royalenfield.ffmechanic.app.core.adb.AdbClient;
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
public final class OtaPipeline_Factory implements Factory<OtaPipeline> {
  private final Provider<AdbClient> adbClientProvider;

  public OtaPipeline_Factory(Provider<AdbClient> adbClientProvider) {
    this.adbClientProvider = adbClientProvider;
  }

  @Override
  public OtaPipeline get() {
    return newInstance(adbClientProvider.get());
  }

  public static OtaPipeline_Factory create(Provider<AdbClient> adbClientProvider) {
    return new OtaPipeline_Factory(adbClientProvider);
  }

  public static OtaPipeline newInstance(AdbClient adbClient) {
    return new OtaPipeline(adbClient);
  }
}
