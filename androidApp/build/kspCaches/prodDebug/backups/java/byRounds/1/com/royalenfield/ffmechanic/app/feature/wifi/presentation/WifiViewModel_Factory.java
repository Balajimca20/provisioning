package com.royalenfield.ffmechanic.app.feature.wifi.presentation;

import android.content.Context;
import com.royalenfield.ffmechanic.app.feature.wifi.domain.WifiUpdateWorkflow;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class WifiViewModel_Factory implements Factory<WifiViewModel> {
  private final Provider<WifiUpdateWorkflow> workflowProvider;

  private final Provider<Context> contextProvider;

  public WifiViewModel_Factory(Provider<WifiUpdateWorkflow> workflowProvider,
      Provider<Context> contextProvider) {
    this.workflowProvider = workflowProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public WifiViewModel get() {
    return newInstance(workflowProvider.get(), contextProvider.get());
  }

  public static WifiViewModel_Factory create(Provider<WifiUpdateWorkflow> workflowProvider,
      Provider<Context> contextProvider) {
    return new WifiViewModel_Factory(workflowProvider, contextProvider);
  }

  public static WifiViewModel newInstance(WifiUpdateWorkflow workflow, Context context) {
    return new WifiViewModel(workflow, context);
  }
}
