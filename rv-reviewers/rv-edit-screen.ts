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
import {css, html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators';
import './rv-filter-section';
import {Section} from './rv-filter-section';
import {fire} from './util';

function getReviewersUrl(repoName: RepoName) {
  return `/projects/${encodeURIComponent(repoName)}/reviewers`;
}

declare global {
  interface HTMLElementTagNameMap {
    'rv-edit-screen': RvEditScreen;
  }
}

@customElement('rv-edit-screen')
export class RvEditScreen extends LitElement {
  @property()
  pluginRestApi!: RestPluginApi;

  @property()
  repoName!: RepoName;

  @property()
  loading = false;

  @property()
  canModifyConfig = false;

  @state()
  editingFilter = false;

  @state()
  filterSections: Section[] = [];

  static override get styles() {
    return [
      css`
        :host {
          padding: var(--spacing-xl);
          display: block;
        }
        .bottomButtons {
          display: flex;
          justify-content: flex-end;
        }
        gr-button {
          margin-left: var(--spacing-m);
        }
        #filterSections {
          width: 100%;
        }
      `,
    ];
  }

  render() {
    return html`
      <div>
        <h3 class="heading-3">Reviewers Config</h3>
        <table id="filterSections">
          <tbody>
            ${this.renderEmpty()}
            ${this.loading
              ? this.renderLoading()
              : this.filterSections.map(s => this.renderSection(s))}
          </tbody>
        </table>
        <div class="bottomButtons">
          <gr-button
            id="addFilterBtn"
            @click="${this.handleCreateSection}"
            ?hidden="${!this.canModifyConfig || this.editingFilter}"
          >
            Add New Filter
          </gr-button>
          <gr-button id="closeButton" @click="${this.handleCloseTap}">
            Close
          </gr-button>
        </div>
      </div>
    `;
  }

  private renderLoading() {
    if (!this.loading) return;
    return html`<tr>
      <td>Loading...</td>
    </tr>`;
  }

  private renderEmpty() {
    if (this.loading || this.filterSections.length > 0) return;
    return html`<tr>
      <td>No filter defined yet.</td>
    </tr>`;
  }

  private renderSection(section: Section) {
    return html`
      <tr>
        <td>
          <rv-filter-section
            .filter="${section.filter}"
            .reviewers="${section.reviewers}"
            .ccs="${section.ccs}"
            .reviewersUrl="${getReviewersUrl(this.repoName)}"
            .repoName="${this.repoName}"
            .pluginRestApi="${this.pluginRestApi}"
            .canModifyConfig="${this.canModifyConfig}"
            @reviewer-changed="${this.handleReviewerChanged}"
          >
          </rv-filter-section>
        </td>
      </tr>
    `;
  }

  connectedCallback() {
    super.connectedCallback();
    this.pluginRestApi
      .get<Section[]>(getReviewersUrl(this.repoName))
      .then(sections => {
        this.filterSections = sections;
      });
  }

  private handleCreateSection() {
    const section = {filter: '', reviewers: [], ccs: [], editing: true};
    this.filterSections = [...this.filterSections, section];
    this.editingFilter = true;
    fire(this, 'fit');
  }

  private handleCloseTap(e: Event) {
    e.preventDefault();
    fire(this, 'close');
  }

  private handleReviewerChanged(e: CustomEvent<Section[]>) {
    // Even if just one reviewer is changed or deleted, then we still completely
    // re-render everything from scratch.
    this.filterSections = e.detail;
    this.editingFilter = false;
    fire(this, 'fit');
  }
}
