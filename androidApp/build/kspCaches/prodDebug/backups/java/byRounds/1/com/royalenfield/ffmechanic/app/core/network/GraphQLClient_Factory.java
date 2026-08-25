package com.royalenfield.ffmechanic.app.core.network;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class GraphQLClient_Factory implements Factory<GraphQLClient> {
  @Override
  public GraphQLClient get() {
    return newInstance();
  }

  public static GraphQLClient_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static GraphQLClient newInstance() {
    return new GraphQLClient();
  }

  private static final class InstanceHolder {
    private static final GraphQLClient_Factory INSTANCE = new GraphQLClient_Factory();
  }
}
