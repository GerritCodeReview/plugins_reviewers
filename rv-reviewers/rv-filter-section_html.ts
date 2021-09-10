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
import './rv-reviewer.js';

export const htmlTemplate = Polymer.html`
    <style include="shared-styles">
      :host {
        display: block;
        margin-bottom: 1em;
      }
      fieldset {
        border: 1px solid var(--border-color);
      }
      .name {
        align-items: center;
        display: flex;
      }
      .name gr-button {
        margin-left: var(--spacing-m);
      }
      .header {
        align-items: center;
        background: var(--table-header-background-color);
        border-bottom: 1px dotted var(--border-color);
        display: flex;
        justify-content: space-between;
        min-height: 3em;
        padding: 0 .7em;
      }
      #addReviewer {
        display: flex;
      }
      #editFilterInput {
        width: 30vw;
        max-width: 500px;
        margin-left: 3px;
      }
      #mainContainer {
        display: block;
      }
    </style>
    <style include="gr-form-styles"></style>
    <fieldset id="section"
        class$="gr-form-styles">
      <div id="mainContainer">
        <div class="header">
          <div class="name">
            <h3>Filter:</h3>
            <iron-input
                id="editFilterInput"
                bind-value="{{filter}}"
                type="text"
                disabled="[[_computeFilterInputDisabled(canModifyConfig, _originalFilter)]]">
              <input
                  id="editFilterInput"
                  bind-value="{{filter}}"
                  is="iron-input"
                  type="text"
                  disabled="[[_computeFilterInputDisabled(canModifyConfig, _originalFilter)]]">
            </iron-input>
            <gr-button
                id="cancelBtn"
                on-tap="_handleCancel"
                hidden$="[[_computeCancelHidden(filter, _originalFilter)]]">Cancel</gr-button>
          </div><!-- name -->
        </div><!-- header -->
        <div class="reviewers">
          <template
              is="dom-repeat"
              items="{{reviewers}}">
            <rv-reviewer
                reviewer="{{item}}"
                type="REVIEWER"
                can-modify-config="[[canModifyConfig]]"
                plugin-rest-api="[[pluginRestApi]]"
                repo-name="[[repoName]]"
                on-reviewer-deleted="_handleReviewerDeleted"
                on-reviewer-added="_handleReviewerAdded">
            </rv-reviewer>
          </template>
          <template
          is="dom-repeat"
          items="{{ccs}}">
            <rv-reviewer
                reviewer="{{item}}"
                type="CC"
                can-modify-config="[[canModifyConfig]]"
                plugin-rest-api="[[pluginRestApi]]"
                repo-name="[[repoName]]"
                on-reviewer-deleted="_handleReviewerDeleted"
                on-reviewer-added="_handleReviewerAdded">
        </rv-reviewer>
      </template>
          <div id="addReviewer">
            <gr-button
                link
                id="addRevBtn"
                on-tap="_handleAddReviewer"
                hidden="[[_computeAddBtnHidden(canModifyConfig, _editingReviewer)]]">Add Reviewer</gr-button>
            <gr-button
                link
                id="addCcBtn"
                on-tap="_handleAddCc"
                hidden="[[_computeAddBtnHidden(canModifyConfig, _editingReviewer)]]">Add Cc</gr-button>
          </div><!-- addReviewer -->
        </div><!-- reviewers -->
      </div>
    </fieldset>
`;
