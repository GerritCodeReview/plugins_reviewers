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
import {htmlTemplate} from './rv-reviewer_html.js';

class RvReviewer extends Polymer.Element {
  /** @returns {string} name of the component */
  static get is() { return 'rv-reviewer'; }

  /** @returns {?} template for this component */
  static get template() { return htmlTemplate; }

  /**
   * Defines properties of the component
   *
   * @returns {?}
   */
  static get properties() {
    return {
      canModifyConfig: Boolean,
      pluginRestAPi: Object,
      repoName: String,
      reviewer: String,
      type: String,
      _header: {
        type: String,
        computed: '_computeHeader(type)',
      },
      _reviewerSearchId: String,
      _queryReviewers: {
        type: Function,
        value() {
          return this._getReviewerSuggestions.bind(this);
        },
      },
      _originalReviewer: String,
      _deleted: Boolean,
      _editing: {
        type: Boolean,
        computed: '_computeEditing(reviewer, _originalReviewer)',
      },
    };
  }

  connectedCallback() {
    super.connectedCallback();
    this._originalReviewer = this.reviewer;
  }

  _computeHeader(type) {
    if (type === 'CC') {
      return 'Cc';
    }
    return 'Reviewer';
  }

  _computeEditing(reviewer, _originalReviewer) {
    if (_originalReviewer === '') {
      return true;
    }
    return reviewer === '';
  }

  _computeDeleteCancel(reviewer, _originalReviewer) {
    return this._computeEditing(reviewer, _originalReviewer) ?
      'Cancel' : 'Delete';
  }

  _computeHideAddButton(reviewer, _originalReviewer) {
    return !(this._computeEditing(reviewer, _originalReviewer)
    && this._reviewerSearchId);
  }

  _computeHideDeleteButton(canModifyConfig) {
    return !canModifyConfig;
  }

  _getReviewerSuggestions(input) {
    if (input.length === 0) { return Promise.resolve([]); }
    const promises = [];
    promises.push(this._getSuggestedGroups(input));
    promises.push(this._getSuggestedAccounts(input));
    return Promise.all(promises).then(result => {
      return result.flat();
    });
  }

  _getSuggestedGroups(input) {
    const suggestUrl = `/groups/?suggest=${input}&p=${this.repoName}`;
    return this.pluginRestApi.get(suggestUrl).then(groups => {
      if (!groups) { return []; }
      const groupSuggestions = [];
      for (const key in groups) {
        if (!groups.hasOwnProperty(key)) { continue; }
        groupSuggestions.push({
          name: key,
          value: key,
        });
      }
      return groupSuggestions;
    });
  }

  _getSuggestedAccounts(input) {
    const suggestUrl = `/accounts/?suggest&q=${input}`;
    return this.pluginRestApi.get(suggestUrl).then(accounts => {
      const accountSuggestions = [];
      let nameAndEmail;
      let value;
      if (!accounts) { return []; }
      for (const key in accounts) {
        if (!accounts.hasOwnProperty(key)) { continue; }
        if (accounts[key].email) {
          nameAndEmail = accounts[key].name +
            ' <' + accounts[key].email + '>';
        } else {
          nameAndEmail = accounts[key].name;
        }
        if (accounts[key].username) {
          value = accounts[key].username;
        } else if (accounts[key].email) {
          value = accounts[key].email;
        } else {
          value = accounts[key]._account_id;
        }
        accountSuggestions.push({
          name: nameAndEmail,
          value,
        });
      }
      return accountSuggestions;
    });
  }

  _handleDeleteCancel() {
    const detail = {editing: this._editing};
    if (this._editing) {
      this.remove();
    }
    this.dispatchEvent(
        new CustomEvent('reviewer-deleted', {detail, bubbles: true}));
  }

  _handleAddReviewer() {
    const detail = {reviewer: this._reviewerSearchId};
    this._originalReviewer = this.reviewer;
    this.dispatchEvent(
        new CustomEvent('reviewer-added', {detail, bubbles: true}));
  }
}

customElements.define(RvReviewer.is, RvReviewer);