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
      editing: {
        type: Boolean,
        value: false
      },
      reviewers: Array,
      _reviewers: Array,
      filter: String,
      _originalFilter: String,
      editing: {
        type: Boolean,
        computed: '_computeEditing(filter, _originalFilter)'
      },
      reviewersUrl: String
    },

    attached() {
      this._updateSection();
    },

    _updateSection() {
      this._reviewers = this.reviewers;
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

    _handleCancel() {
      this.remove();
    },

    _handleReviewerDeleted(e) {
      if (e.detail.editing) {
        this._reviewers.pop();
      } else {
        const index = e.model.index;
        const deleted = this.reviewers[index];
        this._putReviewer(deleted, 'DELETE');
        this.reviewers = this.reviewers.slice(0, index).concat(this.reviewers.slice(index +1));
      }
    },

    _handleReviewerAdded(e) {
      // Remove empty reviewers
      this.reviewers = this.reviewers.filter(r => r !== '');
      this.push('reviewers', e.detail.reviewer);
      this._updateSection();
      this._putReviewer(e.detail.reviewer, 'ADD');
    },

    _putReviewer(reviewer, action) {
      this.plugin.restApi().put(this.reviewersUrl, {
        action: action,
        reviewer: reviewer,
        filter: this.filter
      }).then(result => {
        const detail = {result}
        this.dispatchEvent(new CustomEvent('reviewer-changed', {detail, bubbles: true}));
      })
    },

    _handleAddReviewer() {
      this.push('_reviewers', '');
    },
  });
})();



