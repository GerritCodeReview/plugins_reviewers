// Copyright (C) 2014 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.reviewers.config;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.eclipse.jgit.lib.Config;

/** Global and project local configurations. */
@Singleton
public class GlobalConfig {
  private static final String KEY_ENABLE_REST = "enableREST";
  private static final String KEY_SUGGEST_ONLY = "suggestOnly";
  private static final String KEY_IGNORE_WIP = "ignoreWip";

  private final boolean enableREST;
  private final boolean suggestOnly;
  private final boolean ignoreWip;

  @Inject
  GlobalConfig(PluginConfigFactory cfgFactory, @PluginName String pluginName) {
    Config cfg = cfgFactory.getGlobalPluginConfig(pluginName);
    this.enableREST = cfg.getBoolean(pluginName, null, KEY_ENABLE_REST, true);
    this.suggestOnly = cfg.getBoolean(pluginName, null, KEY_SUGGEST_ONLY, false);
    this.ignoreWip = cfg.getBoolean(pluginName, null, KEY_IGNORE_WIP, true);
  }

  public boolean enableREST() {
    return enableREST;
  }

  public boolean suggestOnly() {
    return suggestOnly;
  }

  public boolean ignoreWip() {
    return ignoreWip;
  }
}
