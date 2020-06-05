/**
 * @license
 * Copyright (C) 2020 The Android Open Source Project
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
import './rv-edit-screen.js';

export const htmlTemplate = Polymer.html`
    <style include="shared-styles">
      :host {
        display: block;
        margin-bottom: var(--spacing-xxl);
      }
      #rvScreenOverlay {
        width: 50em;
        overflow: auto;
      }
    </style>
    <h3>Reviewers Config</h3>
    <gr-button
      on-click="_handleCommandTap"
    >
      Reviewers Config
    </gr-button>
    <gr-overlay id="rvScreenOverlay" with-backdrop>
      <rv-edit-screen
          plugin-rest-api="[[pluginRestApi]]"
          repo-name="[[repoName]]"
          loading="[[_loading]]"
          can-modify-config="[[_canModifyConfig]]"
          on-close="_handleRvEditScreenClose"></rv-edit-screen>
    </gr-overlay>
`;
