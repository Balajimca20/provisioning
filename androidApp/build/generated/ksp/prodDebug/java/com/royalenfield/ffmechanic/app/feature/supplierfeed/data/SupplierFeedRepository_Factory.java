package com.royalenfield.ffmechanic.app.feature.supplierfeed.data;

import com.royalenfield.ffmechanic.app.core.network.GraphQLClient;
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
public final class SupplierFeedRepository_Factory implements Factory<SupplierFeedRepository> {
  private final Provider<GraphQLClient> graphQLClientProvider;

  public SupplierFeedRepository_Factory(Provider<GraphQLClient> graphQLClientProvider) {
    this.graphQLClientProvider = graphQLClientProvider;
  }

  @Override
  public SupplierFeedRepository get() {
    return newInstance(graphQLClientProvider.get());
  }

  public static SupplierFeedRepository_Factory create(
      Provider<GraphQLClient> graphQLClientProvider) {
    return new SupplierFeedRepository_Factory(graphQLClientProvider);
  }

  public static SupplierFeedRepository newInstance(GraphQLClient graphQLClient) {
    return new SupplierFeedRepository(graphQLClient);
  }
}
