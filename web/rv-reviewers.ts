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
import {RepoName} from '@gerritcodereview/typescript-api/rest-api';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {PluginApi} from '@gerritcodereview/typescript-api/plugin';
import {customElement, property, query, state} from 'lit/decorators';
import {css, html, LitElement} from 'lit';
import './rv-edit-screen';

// TODO: This should be defined and exposed by @gerritcodereview/typescript-api
type GrOverlay = Element & {
  open(): void;
  close(): void;
  fit(): void;
};

declare global {
  interface HTMLElementTagNameMap {
    'rv-reviewers': RvReviewers;
  }
}

declare interface AccountCapabilityInfo {
  'reviewers-modifyReviewersConfig'?: boolean;
}

type ProjectAccessInfoMap = {[projectName: string]: ProjectAccessInfo};

declare interface ProjectAccessInfo {
  is_owner?: boolean;
}

@customElement('rv-reviewers')
export class RvReviewers extends LitElement {
  @query('#rvScreenOverlay')
  rvScreenOverlay?: GrOverlay;

  /** Guaranteed to be set by the `repo-command` endpoint. */
  @property({type: Object})
  plugin!: PluginApi;

  /** Guaranteed to be set by the `repo-command` endpoint. */
  @property({type: String})
  repoName!: RepoName;

  @state()
  pluginRestApi!: RestPluginApi;

  @state()
  canModifyConfig = false;

  @state()
  loading = true;

  static override get styles() {
    return [
      css`
        :host {
          display: block;
          margin-bottom: var(--spacing-xxl);
        }
        #rvScreenOverlay {
          width: 50em;
          overflow: auto;
        }
      `,
    ];
  }

  render() {
    return html`
      <h3 class="heading-3">Reviewers Config</h3>
      <gr-button @click="${() => this.rvScreenOverlay?.open()}">
        Reviewers Config
      </gr-button>
      <gr-overlay id="rvScreenOverlay" with-backdrop>
        <rv-edit-screen
          .pluginRestApi="${this.pluginRestApi}"
          .repoName="${this.repoName}"
          .loading="${this.loading}"
          .canModifyConfig="${this.canModifyConfig}"
          @close="${() => this.rvScreenOverlay?.close()}"
          @fit="${() => this.rvScreenOverlay?.fit()}"
        >
        </rv-edit-screen>
      </gr-overlay>
    `;
  }

  connectedCallback() {
    super.connectedCallback();
    this.pluginRestApi = this.plugin.restApi();
    const p1 = this.getRepoAccess(this.repoName).then(access => {
      if (access[this.repoName]?.is_owner) {
        this.canModifyConfig = true;
      }
    });
    const p2 = this.getCapabilities().then(capabilities => {
      if (capabilities['reviewers-modifyReviewersConfig']) {
        this.canModifyConfig = true;
      }
    });
    Promise.all([p1, p2]).then(() => (this.loading = false));
  }

  getRepoAccess(repoName: RepoName) {
    return this.pluginRestApi.get<ProjectAccessInfoMap>(
      '/access/?project=' + encodeURIComponent(repoName)
    );
  }

  getCapabilities() {
    return this.pluginRestApi.get<AccountCapabilityInfo>(
      '/accounts/self/capabilities'
    );
  }
}
