package com.googlesource.gerrit.plugins.reviewers.client;

import com.google.gerrit.plugin.client.Plugin;
import com.google.gerrit.plugin.client.PluginEntryPoint;

public class ReviewersPlugin extends PluginEntryPoint {
  @Override
  public void onPluginLoad() {
    Plugin.get().screenRegex(".*", new AddReviewersScreen.Factory());
  }
}
