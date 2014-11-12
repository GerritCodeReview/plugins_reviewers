package com.googlesource.gerrit.plugins.reviewers;

import com.google.common.collect.Lists;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.webui.TopMenu;
import com.google.inject.Inject;

import java.util.Collections;
import java.util.List;

public class ReviewersTopMenu implements TopMenu {
  private final List<MenuEntry> menuEntries;

  @Inject
  ReviewersTopMenu(@PluginName String pluginName) {
    menuEntries = Lists.newArrayList();
    menuEntries.add(new MenuEntry("Projects", Collections.singletonList(
        new MenuItem("Reviewers", "#/x/" + pluginName + "/${projectName}", "_self"))));
  }

  @Override
  public List<MenuEntry> getEntries() {
    return this.menuEntries;
  }

}
