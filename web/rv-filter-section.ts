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
import {css, CSSResult, html, LitElement, nothing} from 'lit';
import {customElement, property, query, state} from 'lit/decorators';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import {RepoName} from '@gerritcodereview/typescript-api/rest-api';
import '@gerritcodereview/typescript-api/gerrit';
import './rv-reviewer';
import {
  ReviewerAddedEventDetail,
  ReviewerDeletedEventDetail,
  Type,
} from './rv-reviewer';
import {fire} from './util';

enum Action {
  ADD = 'ADD',
  REMOVE = 'REMOVE',
}

export interface Section {
  filter: string;
  reviewers: string[];
  ccs: string[];
  editing: boolean;
}

declare global {
  interface HTMLElementTagNameMap {
    'rv-filter-section': RvFilterSection;
  }
}

@customElement('rv-filter-section')
export class RvFilterSection extends LitElement {
  @query('#filterInput')
  filterInput?: HTMLInputElement;

  @property()
  pluginRestApi!: RestPluginApi;

  @property()
  repoName!: RepoName;

  @property()
  reviewers: string[] = [];

  @property()
  ccs: string[] = [];

  @property()
  filter = '';

  @property()
  canModifyConfig = false;

  @property({type: String})
  reviewersUrl = '';

  /**
   * If a filter was already set initially, then you cannot "cancel" creating
   * this filter.
   */
  @state()
  originalFilter = '';

  /** While a reviewer is being edited you cannot add another. */
  @state()
  editingReviewer = false;

  override connectedCallback() {
    super.connectedCallback();
    this.originalFilter = this.filter;
  }

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      css`
        :host {
          display: block;
          margin-bottom: 1em;
        }
        #container {
          display: block;
          border: 1px solid var(--border-color);
        }
        #filter {
          align-items: center;
          background: var(--table-header-background-color);
          border-bottom: 1px solid var(--border-color);
          display: flex;
          justify-content: space-between;
          min-height: 3em;
          padding: 0 var(--spacing-m);
        }
        #filterInput {
          width: 30vw;
          max-width: 500px;
          margin-left: var(--spacing-s);
        }
        gr-button {
          margin-left: var(--spacing-m);
        }
        #addReviewer {
          display: flex;
          padding: var(--spacing-s) 0;
        }
      `,
    ];
  }

  override render() {
    return html`
      <div id="container">
        ${this.renderFilter()}
        <div>
          ${this.reviewers.map(item =>
            this.renderReviewer(item, Type.REVIEWER)
          )}
          ${this.ccs.map(item => this.renderReviewer(item, Type.CC))}
          ${this.renderAddReviewer()}
        </div>
      </div>
    `;
  }

  private renderFilter() {
    return html`
      <div id="filter">
        <span class="heading-3">Filter</span>
        <input
          id="filterInput"
          value="${this.filter}"
          @input="${this.onFilterInput}"
          ?disabled="${!this.canModifyConfig || this.originalFilter !== ''}"
        />
        <gr-button
          @click="${() => this.remove()}"
          ?hidden="${this.originalFilter !== '' && this.filter !== ''}"
        >
          Cancel
        </gr-button>
      </div>
    `;
  }

  private renderAddReviewer() {
    if (!this.canModifyConfig) return nothing;
    if (this.editingReviewer) return nothing;
    return html`
      <div id="addReviewer">
        <gr-button
          link
          @click="${this.handleAddReviewer}"
          ?disabled="${this.filter === ''}"
        >
          Add Reviewer
        </gr-button>
        <gr-button
          link
          @click="${this.handleAddCc}"
          ?disabled="${this.filter === ''}"
        >
          Add CC
        </gr-button>
      </div>
    `;
  }

  private renderReviewer(reviewer: string, type: Type) {
    return html`
      <rv-reviewer
        .reviewer="${reviewer}"
        .type="${type}"
        .canModifyConfig="${this.canModifyConfig}"
        .pluginRestApi="${this.pluginRestApi}"
        .repoName="${this.repoName}"
        @reviewer-deleted="${(e: CustomEvent<ReviewerDeletedEventDetail>) =>
          this.handleReviewerDeleted(e, reviewer)}"
        @reviewer-added="${(e: CustomEvent<ReviewerAddedEventDetail>) =>
          this.handleReviewerAdded(e)}"
      >
      </rv-reviewer>
    `;
  }

  private onFilterInput() {
    this.filter = this.filterInput?.value ?? '';
  }

  private handleReviewerDeleted(
    e: CustomEvent<ReviewerDeletedEventDetail>,
    reviewer: string
  ) {
    const {type, editing} = e.detail;
    if (editing) {
      // Just cancelling edit. Nothing was persisted yet, so nothing to delete.
      if (type === Type.CC) {
        this.ccs = [...this.ccs.slice(0, -1)];
      } else {
        this.reviewers = [...this.reviewers.slice(0, -1)];
      }
      this.editingReviewer = false;
    } else {
      // The reviewer was not in edit mode, but DELETE was clicked.
      this.postReviewer(reviewer, Action.REMOVE, type);
    }
  }

  private handleReviewerAdded(e: CustomEvent<ReviewerAddedEventDetail>) {
    this.editingReviewer = false;
    this.postReviewer(e.detail.reviewer, Action.ADD, e.detail.type).catch(
      err => {
        fire(this, 'show-alert', {message: err});
        throw err;
      }
    );
  }

  private postReviewer(reviewer: string, action: Action, type: Type) {
    if (this.filter === '') throw new Error('empty filter');
    if (reviewer === '') throw new Error('empty reviewer');
    return this.pluginRestApi
      .post<Section[]>(this.reviewersUrl, {
        action,
        reviewer,
        type,
        filter: this.filter,
      })
      .then((sections: Section[]) => {
        // Even if just one reviewer is changed or deleted, we will get the
        // the complete list of sections back from the server, and we dispatch
        // this event such that the entire dialog re-renders from scratch.
        // Lit is smart enough to re-use the component though, so we also want
        // to re-initialize the state here:
        this.editingReviewer = false;
        this.originalFilter = this.filter;
        fire(this, 'reviewer-changed', sections);
      });
  }

  private handleAddReviewer() {
    this.reviewers = [...this.reviewers, ''];
    this.editingReviewer = true;
  }

  private handleAddCc() {
    this.ccs = [...this.ccs, ''];
    this.editingReviewer = true;
  }
}
