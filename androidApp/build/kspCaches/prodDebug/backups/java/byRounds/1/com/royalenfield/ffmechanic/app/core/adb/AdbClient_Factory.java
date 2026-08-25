package com.royalenfield.ffmechanic.app.core.adb;

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
public final class AdbClient_Factory implements Factory<AdbClient> {
  private final Provider<AdbKeyPairProvider> keyPairProvider;

  public AdbClient_Factory(Provider<AdbKeyPairProvider> keyPairProvider) {
    this.keyPairProvider = keyPairProvider;
  }

  @Override
  public AdbClient get() {
    return newInstance(keyPairProvider.get());
  }

  public static AdbClient_Factory create(Provider<AdbKeyPairProvider> keyPairProvider) {
    return new AdbClient_Factory(keyPairProvider);
  }

  public static AdbClient newInstance(AdbKeyPairProvider keyPairProvider) {
    return new AdbClient(keyPairProvider);
  }
}
