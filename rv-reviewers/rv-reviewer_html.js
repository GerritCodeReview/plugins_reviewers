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
export const htmlTemplate = Polymer.html`
    <style include="shared-styles">
      :host {
        display: block;
        padding: var(--spacing-s) 0;
      }
      #editReviewerInput {
        display: block;
        width: 250px;
      }
      .reviewerRow {
        align-items: center;
        display: flex;
      }
      #reviewerHeader,
      #editReviewerInput,
      #deleteCancelBtn,
      #addBtn,
      #reviewerField {
        margin-left: var(--spacing-m);
      }
      #reviewerField {
        width: 250px;
        text-indent: 1px;
        border: 1px solid var(--border-color);
      }
    </style>
    <style include="gr-form-styles"></style>
    <div class="reviewerRow">
      <h4 id="reviewerHeader">[[_header]]:</h4>
      <template is="dom-if" if="[[_computeEditing(reviewer, _originalReviewer)]]">
        <span class="value">
            <!--
              TODO:
              Investigate whether we could reuse gr-account-list.
              If the REST API returns AccountInfo instead of an account
              identifier String we should be able to use gr-account-list(size=1)
              for all reviewers, including those who are non-editable
              (#reviewerField below) and align the plugin with how accounts
              are displayed in core Gerrit's UI.
            -->
            <gr-autocomplete
                id="editReviewerInput"
                text="{{reviewer}}"
                value="{{_reviewerSearchId}}"
                query="[[_queryReviewers]]"
                placeholder="Name Or Email">
            </gr-autocomplete>
        </span>
      </template>
      <template is="dom-if" if="[[!_computeEditing(reviewer, _originalReviewer)]]">
        <td id="reviewerField">[[reviewer]]</td>
      </template>
      <gr-button
          id="deleteCancelBtn"
          on-tap="_handleDeleteCancel"
          hidden$="[[_computeHideDeleteButton(canModifyConfig)]]"
          >[[_computeDeleteCancel(reviewer, _originalReviewer)]]</gr-button>
      <gr-button
          id="addBtn"
          on-tap="_handleAddReviewer"
          hidden$="[[_computeHideAddButton(reviewer, _originalReviewer)]]">Add</gr-button>
    </div> <!-- reviewerRow -->
`;