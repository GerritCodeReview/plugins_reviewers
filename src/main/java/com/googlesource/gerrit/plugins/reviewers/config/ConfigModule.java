package com.googlesource.gerrit.plugins.reviewers.config;

import com.google.gerrit.extensions.config.FactoryModule;

public class ConfigModule extends FactoryModule {

  @Override
  protected void configure() {
    factory(ForProject.Factory.class);
    factory(ReviewerFilterCollection.Factory.class);
  }
}
