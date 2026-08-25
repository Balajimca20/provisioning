package com.royalenfield.ffmechanic.app.core.adb;

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
public final class AdbKeyPairProvider_Factory implements Factory<AdbKeyPairProvider> {
  private final Provider<Context> contextProvider;

  public AdbKeyPairProvider_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public AdbKeyPairProvider get() {
    return newInstance(contextProvider.get());
  }

  public static AdbKeyPairProvider_Factory create(Provider<Context> contextProvider) {
    return new AdbKeyPairProvider_Factory(contextProvider);
  }

  public static AdbKeyPairProvider newInstance(Context context) {
    return new AdbKeyPairProvider(context);
  }
}
