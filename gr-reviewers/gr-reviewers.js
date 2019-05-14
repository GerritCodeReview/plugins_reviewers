// Copyright (C) 2019 The Android Open Source Project
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
(function() {
  Polymer({
    is: 'gr-reviewers',

    properties: {
      pluginRestApi: Object,
      repoName: String,
      _canModifyConfig: {
        type: Boolean,
        computed: '_computeCanModifyConfig(_isOwner, _hasModifyCapability)',
      },
      _loading: {
        typs: Boolean,
        value: true,
      },
      _isOwner: {
        type: Boolean,
        value: false,
      },
      _hasModifyCapability: {
        type: Boolean,
        value: false,
      }
    },

    attached() {
      this.pluginRestApi = this.plugin.restApi();
      this._setCanModifyConfig();
    },

    _handleCommandTap() {
      this.$.rvScreenOverlay.open();
    },

    _handleRvEditScreenClose() {
      this.$.rvScreenOverlay.close();
    },

    _setCanModifyConfig() {
      const promises = [];
      promises.push(this._getRepoAccess(this.repoName).then( access => {
          if (access && access[this.repoName] && access[this.repoName].is_owner) {
            this._isOwner = true;
          }
        }));
      promises.push(this._getCapabilities().then(capabilities => {
        if (capabilities['reviewers-modifyReviewersConfig']) {
          this._hasModifyCapability = true;
        }
        console.log("Done")
      }));
      Promise.all(promises).then(() => {
        console.log("Done");
        this._loading = false;
      });
    },

    _computeCanModifyConfig(isOwner, hasModifyCapability) {
      return isOwner || hasModifyCapability;
    },

    _getRepoAccess(repoName) {
      return this.pluginRestApi.get('/access/?project=' + encodeURIComponent(repoName));
    },

    _getCapabilities() {
      return this.pluginRestApi.get('/accounts/self/capabilities');
    }
  });
})();