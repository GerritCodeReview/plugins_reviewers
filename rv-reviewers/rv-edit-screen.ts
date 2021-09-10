/**
 * @license
 * Copyright (C) 2019 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {htmlTemplate} from './rv-edit-screen_html.js';

class RvEditScreen extends Polymer.Element {
  /** @returns {string} name of the component */
  static get is() { return 'rv-edit-screen'; }

  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
      pluginRestApi: {
        type: Object,
        observer: '_loadFilterSections',
      },
      repoName: String,
      loading: Boolean,
      canModifyConfig: Boolean,
      _editingFilter: {
        type: Boolean,
        value: false,
      },
      _filterSections: Array,
    };
  }

  _loadFilterSections() {
    this.pluginRestApi.get(this._getReviewersUrl(this.repoName))
        .then(filterSections => {
          this._filterSections = filterSections;
        });
  }

  _computeAddFilterBtnHidden(canModifyConfig, editingFilter) {
    return !canModifyConfig || editingFilter;
  }

  _computeLoadingClass(loading) {
    return loading ? 'loading' : '';
  }

  _getReviewersUrl(repoName) {
    return `/projects/${encodeURIComponent(repoName)}/reviewers`;
  }

  _handleCreateSection() {
    const section = {filter: '', reviewers: [], ccs: [], editing: true};
    this._editingFilter = true;
    this.push('_filterSections', section);
  }

  _handleCloseTap(e) {
    e.preventDefault();
    this.dispatchEvent(new CustomEvent('close', {
      composed: true, bubbles: false,
    }));
  }

  _handleReviewerChanged(e) {
    this._filterSections = e.detail.result;
    this._editingFilter = false;
  }
}

customElements.define(RvEditScreen.is, RvEditScreen);
