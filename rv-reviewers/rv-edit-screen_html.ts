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
import './rv-filter-section.js';

export const htmlTemplate = Polymer.html`
    <style include="shared-styles"></style>
    <style include="gr-menu-page-styles"></style>
    <style include="gr-subpage-styles">
      :host {
        padding: var(--spacing-xl);
      }
      .bottomButtons {
        display: flex;
      }
      #closeButton {
        float: right;
      }
      #filterSections {
          width: 100%;
      }
      header {
        border-bottom: 1px solid var(--border-colo);
        flex-shrink: 0;
        font-weight: var(--font-weight-bold)
      }
    </style>
    <div>
      <header>Reviewers Config</header>
      <table id="filterSections">
        <tr>
          <th>Filter Sections</th>
        </tr>
        <tr id="loading" class$="loadingMsg [[_computeLoadingClass(loading)]]">
          <td>Loading...</td>
        </tr>
        <tbody class$="[[_computeLoadingClass(loading)]]">
          <tr>
            <template
                is="dom-repeat"
                items="[[_filterSections]]"
                as="section">
              <rv-filter-section
                  filter="[[section.filter]]"
                  reviewers="[[section.reviewers]]"
                  ccs="[[section.ccs]]"
                  editing="[[section.editing]]"
                  reviewers-url="[[_getReviewersUrl(repoName)]]"
                  repo-name="[[repoName]]"
                  plugin-rest-api="[[pluginRestApi]]"
                  can-modify-config="[[canModifyConfig]]"
                  on-reviewer-changed="_handleReviewerChanged"></rv-filter-section>
            </template>
          </tr>
        </tbody>
      </table>
      <div class="bottomButtons">
        <gr-button id="closeButton" on-tap="_handleCloseTap">Close</gr-button>
        <gr-button
            id="addFilterBtn"
            on-tap="_handleCreateSection"
            hidden="[[_computeAddFilterBtnHidden(canModifyConfig, _editingFilter)]]">Add New Filter</gr-button>
      </div>
    </div>
`;
