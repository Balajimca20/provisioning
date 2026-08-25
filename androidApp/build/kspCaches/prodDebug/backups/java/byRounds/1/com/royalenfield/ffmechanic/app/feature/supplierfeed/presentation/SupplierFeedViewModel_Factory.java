package com.royalenfield.ffmechanic.app.feature.supplierfeed.presentation;

import com.royalenfield.ffmechanic.app.feature.supplierfeed.data.SupplierFeedRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class SupplierFeedViewModel_Factory implements Factory<SupplierFeedViewModel> {
  private final Provider<SupplierFeedRepository> repositoryProvider;

  public SupplierFeedViewModel_Factory(Provider<SupplierFeedRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SupplierFeedViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static SupplierFeedViewModel_Factory create(
      Provider<SupplierFeedRepository> repositoryProvider) {
    return new SupplierFeedViewModel_Factory(repositoryProvider);
  }

  public static SupplierFeedViewModel newInstance(SupplierFeedRepository repository) {
    return new SupplierFeedViewModel(repository);
  }
}
