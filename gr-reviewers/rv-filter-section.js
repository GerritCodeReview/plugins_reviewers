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
    is: 'rv-filter-section',

    properties: {
      plugin: Object,
      reviewers: Array,
      filter: String,
      canModifyConfig: Boolean,
      _originalFilter: String,
      _editingReviewer: {
        type: Boolean,
        value: false,
      },
      reviewersUrl: String
    },

    attached() {
      this._updateSection();
    },

    _updateSection() {
      this._originalFilter = this.filter
    },

    _computeEditing(filter, _originalFilter) {
      if (_originalFilter === '') {
        return true;
      }
      return filter === '';
    },

    _computeCancelHidden(filter, _originalFilter) {
      return !this._computeEditing(filter, _originalFilter);
    },

    _computeAddBtnHidden(canModifyConfig, editingReviewer) {
      return !(canModifyConfig && !editingReviewer);
    },

    _computeFilterInputDisabled(canModifyConfig, originalFilter) {
      return !canModifyConfig || originalFilter !== '';
    },

    _handleCancel() {
      this.remove();
    },

    _handleReviewerDeleted(e) {
      if (e.detail.editing) {
        this.reviewers.pop();
        this._editingReviewer = false;
      } else {
        const index = e.model.index;
        const deleted = this.reviewers[index];
        this._putReviewer(deleted, 'DELETE');
        if (!reviewers) {
          this.remove();
        }
      }
    },

    _handleReviewerAdded(e) {
      this._putReviewer(e.detail.reviewer, 'ADD');
      this._updateSection();
      this._editingReviewer = false;
    },

    _putReviewer(reviewer, action) {
      this.plugin.restApi().put(this.reviewersUrl, {
        action: action,
        reviewer: reviewer,
        filter: this.filter
      }).then(result => {
        const detail = {result};
        this.dispatchEvent(new CustomEvent('reviewer-changed', {detail, bubbles: true}));
      })
    },

    _handleAddReviewer() {
      this.push('reviewers', '');
      this._editingReviewer = true;
    },
  });
})();
