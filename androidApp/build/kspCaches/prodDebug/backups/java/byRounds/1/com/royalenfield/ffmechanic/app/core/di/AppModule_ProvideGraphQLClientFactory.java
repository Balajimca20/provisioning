package com.royalenfield.ffmechanic.app.core.di;

import com.royalenfield.ffmechanic.app.core.network.GraphQLClient;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AppModule_ProvideGraphQLClientFactory implements Factory<GraphQLClient> {
  @Override
  public GraphQLClient get() {
    return provideGraphQLClient();
  }

  public static AppModule_ProvideGraphQLClientFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GraphQLClient provideGraphQLClient() {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGraphQLClient());
  }

  private static final class InstanceHolder {
    private static final AppModule_ProvideGraphQLClientFactory INSTANCE = new AppModule_ProvideGraphQLClientFactory();
  }
}
