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
import {htmlTemplate} from './rv-filter-section_html.js';

class RvFilterSection extends Polymer.Element {
  /** @returns {string} name of the component */
  static get is() { return 'rv-filter-section'; }

  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
      pluginRestApi: Object,
      repoName: String,
      reviewers: Array,
      filter: String,
      canModifyConfig: Boolean,
      _originalFilter: String,
      _editingReviewer: {
        type: Boolean,
        value: false,
      },
      reviewersUrl: String,
    };
  }

  connectedCallback() {
    super.connectedCallback();
    this._updateSection();
  }

  _updateSection() {
    this._originalFilter = this.filter;
  }

  _computeEditing(filter, _originalFilter) {
    if (_originalFilter === '') {
      return true;
    }
    return filter === '';
  }

  _computeCancelHidden(filter, _originalFilter) {
    return !this._computeEditing(filter, _originalFilter);
  }

  _computeAddBtnHidden(canModifyConfig, editingReviewer) {
    return !(canModifyConfig && !editingReviewer);
  }

  _computeFilterInputDisabled(canModifyConfig, originalFilter) {
    return !canModifyConfig || originalFilter !== '';
  }

  _handleCancel() {
    this.remove();
  }

  _handleReviewerDeleted(e) {
    if (e.detail.editing) {
      this.reviewers.pop();
      this._editingReviewer = false;
    } else {
      const index = e.model.index;
      const deleted = this.reviewers[index];
      this._putReviewer(deleted, 'REMOVE');
    }
  }

  _handleReviewerAdded(e) {
    this._editingReviewer = false;
    this._putReviewer(e.detail.reviewer, 'ADD').catch(err => {
      this.dispatchEvent(new CustomEvent('show-alert', {
        detail: {
          message: err,
        },
        composed: true, bubbles: true,
      }));
      throw err;
    });
  }

  _putReviewer(reviewer, action) {
    return this.pluginRestApi.put(this.reviewersUrl, {
      action,
      reviewer,
      filter: this.filter,
    }).then(result => {
      const detail = {result};
      this.dispatchEvent(
          new CustomEvent('reviewer-changed', {detail, bubbles: true}));
    });
  }

  _handleAddReviewer() {
    this.push('reviewers', '');
    this._editingReviewer = true;
  }
}

customElements.define(RvFilterSection.is, RvFilterSection);
