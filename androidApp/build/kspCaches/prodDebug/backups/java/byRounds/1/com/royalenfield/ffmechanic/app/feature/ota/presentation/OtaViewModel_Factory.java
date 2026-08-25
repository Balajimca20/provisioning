package com.royalenfield.ffmechanic.app.feature.ota.presentation;

import com.royalenfield.ffmechanic.app.feature.ota.domain.OtaPipeline;
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
public final class OtaViewModel_Factory implements Factory<OtaViewModel> {
  private final Provider<OtaPipeline> pipelineProvider;

  public OtaViewModel_Factory(Provider<OtaPipeline> pipelineProvider) {
    this.pipelineProvider = pipelineProvider;
  }

  @Override
  public OtaViewModel get() {
    return newInstance(pipelineProvider.get());
  }

  public static OtaViewModel_Factory create(Provider<OtaPipeline> pipelineProvider) {
    return new OtaViewModel_Factory(pipelineProvider);
  }

  public static OtaViewModel newInstance(OtaPipeline pipeline) {
    return new OtaViewModel(pipeline);
  }
}
