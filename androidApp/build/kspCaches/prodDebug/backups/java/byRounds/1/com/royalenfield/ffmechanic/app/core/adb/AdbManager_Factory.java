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
public final class AdbManager_Factory implements Factory<AdbManager> {
  private final Provider<AdbKeyPairProvider> keyPairProvider;

  public AdbManager_Factory(Provider<AdbKeyPairProvider> keyPairProvider) {
    this.keyPairProvider = keyPairProvider;
  }

  @Override
  public AdbManager get() {
    return newInstance(keyPairProvider.get());
  }

  public static AdbManager_Factory create(Provider<AdbKeyPairProvider> keyPairProvider) {
    return new AdbManager_Factory(keyPairProvider);
  }

  public static AdbManager newInstance(AdbKeyPairProvider keyPairProvider) {
    return new AdbManager(keyPairProvider);
  }
}
