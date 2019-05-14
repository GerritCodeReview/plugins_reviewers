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
    is : "rv-reviewer",

    properties: {
      reviewer: String,
      canModifyConfig: Boolean,
      _originalReviewer: String,
      _deleted: Boolean,
      _editing: {
        type: Boolean,
        computed: '_computeEditing(reviewer, _originalReviewer)'
      }
    },

    attached() {
      this._originalReviewer = this.reviewer;
    },

    _computeReviewerDisabled(reviewer, _originalReviewer) {
      return !this._computeEditing(reviewer, this._originalReviewer);
    },

    _computeEditing(reviewer, _originalReviewer) {
      if (_originalReviewer === '') {
        return true;
      }
      return reviewer === '';
    },

    _computeDeleteCancel(reviewer, _originalReviewer) {
      return this._computeEditing(reviewer, _originalReviewer) ? 'Cancel' : 'Delete';
    },

    _computeHideAddButton(reviewer, _originalReviewer) {
      return !(this._computeEditing(reviewer, _originalReviewer) && this.$.editReviewerInput.value)
    },

    _computeHideDeleteButton(canModifyConfig) {
      return !canModifyConfig;
    },

    _handleDeleteCancel() {
      const detail = {editing: this._editing};
      if (this._editing) {
        this.remove();
      }
      this.dispatchEvent(new CustomEvent('reviewer-deleted', {detail, bubbles: true}));
    },

    _handleAddReviewer() {
      const detail = {reviewer: this.reviewer};
      this._originalReviewer = this.reviewer;
      this.dispatchEvent(new CustomEvent('reviewer-added', {detail, bubbles: true}));
    },
  });
})();
