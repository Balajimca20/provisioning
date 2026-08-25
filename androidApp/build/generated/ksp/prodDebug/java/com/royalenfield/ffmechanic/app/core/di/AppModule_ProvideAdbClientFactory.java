package com.royalenfield.ffmechanic.app.core.di;

import com.royalenfield.ffmechanic.app.core.adb.AdbClient;
import com.royalenfield.ffmechanic.app.core.adb.AdbKeyPairProvider;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideAdbClientFactory implements Factory<AdbClient> {
  private final Provider<AdbKeyPairProvider> keyPairProvider;

  public AppModule_ProvideAdbClientFactory(Provider<AdbKeyPairProvider> keyPairProvider) {
    this.keyPairProvider = keyPairProvider;
  }

  @Override
  public AdbClient get() {
    return provideAdbClient(keyPairProvider.get());
  }

  public static AppModule_ProvideAdbClientFactory create(
      Provider<AdbKeyPairProvider> keyPairProvider) {
    return new AppModule_ProvideAdbClientFactory(keyPairProvider);
  }

  public static AdbClient provideAdbClient(AdbKeyPairProvider keyPairProvider) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAdbClient(keyPairProvider));
  }
}
