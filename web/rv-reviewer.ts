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
import {customElement, property, state} from 'lit/decorators';
import {css, CSSResult, html, LitElement} from 'lit';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';
import '@gerritcodereview/typescript-api/gerrit';
import {
  AccountInfo,
  GroupInfo,
  RepoName,
} from '@gerritcodereview/typescript-api/rest-api';
import {fire} from './util';

declare global {
  interface HTMLElementTagNameMap {
    'rv-reviewer': RvReviewer;
  }
}

export enum Type {
  REVIEWER = 'REVIEWER',
  CC = 'CC',
}

export interface ReviewerDeletedEventDetail {
  /**
   * If true, then this means a reviewer addition was just canceled. Not server
   * update required.
   * If false, then the entry has to be deleted server side by the event
   * handler.
   */
  editing: boolean;
  type: Type;
}

export interface ReviewerAddedEventDetail {
  reviewer: string;
  type: Type;
}

type GroupNameToInfo = {[name: string]: GroupInfo};

interface NameValue {
  name: string;
  value: string;
}

function computeValue(account: AccountInfo): string | undefined {
  if (account.username) {
    return account.username;
  }
  if (account.email) {
    return account.email;
  }
  return String(account._account_id);
}

function computeName(account: AccountInfo): string | undefined {
  if (account.email) {
    return `${account.name} <${account.email}>`;
  }
  return account.name;
}

@customElement('rv-reviewer')
export class RvReviewer extends LitElement {
  /**
   * Fired when the 'CANCEL' or 'DELETE' button for a reviewer was clicked.
   *
   * @event reviewer-deleted
   */

  /**
   * Fired when the 'ADD' button for a reviewer was clicked.
   *
   * @event reviewer-added
   */

  @property()
  canModifyConfig = false;

  @property()
  pluginRestApi!: RestPluginApi;

  @property()
  repoName!: RepoName;

  @property()
  type = Type.REVIEWER;

  /**
   * This is the value that is persisted on the server side. For new reviewers
   * this is empty until the user clicks "ADD" and the data was saved.
   */
  @property()
  reviewer = '';

  /**
   * This is value that the user has picked from the auto-completion. It will
   * be used for saving (when the user clicks "ADD") and then assigned to the
   * `reviewer` property.
   */
  @state()
  selectedReviewer = '';

  static override get styles() {
    return [
      window.Gerrit.styles.font as CSSResult,
      css`
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
      `,
    ];
  }

  override render() {
    return html`
      <div class="reviewerRow">
        <span class="heading-3" id="reviewerHeader">
          ${this.type === Type.CC ? 'CC' : 'Reviewer'}
        </span>
        ${this.isEditing()
          ? this.renderAutocomplete()
          : html`<td id="reviewerField">${this.reviewer}</td>`}
        <gr-button
          id="deleteCancelBtn"
          @click="${this.handleDeleteCancel}"
          ?hidden="${!this.canModifyConfig}"
        >
          ${this.isEditing() ? 'Cancel' : 'Delete'}
        </gr-button>
        <gr-button
          id="addBtn"
          @click="${this.handleAddReviewer}"
          ?hidden="${!this.isEditing() || !this.selectedReviewer}"
        >
          Add
        </gr-button>
      </div>
    `;
  }

  renderAutocomplete() {
    return html`
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
          .query="${(input: string) => this.getReviewerSuggestions(input)}"
          .placeholder="Name Or Email"
          @value-changed="${this.onReviewerSelected}"
        >
        </gr-autocomplete>
      </span>
    `;
  }

  onReviewerSelected(e: CustomEvent<{value: string}>) {
    if (!e.detail.value) return;
    this.selectedReviewer = e.detail.value;
  }

  /**
   * "Editing" actually just means "adding". This component does not allow
   * editing. You can only add new entries or delete existing ones.
   */
  isEditing() {
    return this.reviewer === '';
  }

  getReviewerSuggestions(input: string): Promise<NameValue[]> {
    if (input.length === 0) return Promise.resolve([]);
    const p1 = this.getSuggestedGroups(input);
    const p2 = this.getSuggestedAccounts(input);
    return Promise.all([p1, p2]).then(result => result.flat());
  }

  getSuggestedGroups(input: string): Promise<NameValue[]> {
    const suggestUrl = `/groups/?suggest=${input}&p=${this.repoName}`;
    return this.pluginRestApi.get<GroupNameToInfo>(suggestUrl).then(groups => {
      if (!groups) return [];
      return Object.keys(groups)
        .filter(name => !name.startsWith('user/'))
        .filter(name => !groups[name].id.startsWith('global%3A'))
        .map(name => {
          return {name, value: name};
        });
    });
  }

  getSuggestedAccounts(input: string): Promise<NameValue[]> {
    const suggestUrl = `/accounts/?suggest&q=${input}`;
    return this.pluginRestApi.get<AccountInfo[]>(suggestUrl).then(accounts => {
      const accountSuggestions: NameValue[] = [];
      if (!accounts) return [];
      for (const account of accounts) {
        const name = computeName(account);
        const value = computeValue(account);
        if (!name || !value) continue;
        accountSuggestions.push({name, value});
      }
      return accountSuggestions;
    });
  }

  handleDeleteCancel() {
    const detail: ReviewerDeletedEventDetail = {
      editing: this.isEditing(),
      type: this.type,
    };
    if (this.isEditing()) {
      this.remove();
    }
    fire(this, 'reviewer-deleted', detail);
  }

  handleAddReviewer() {
    const detail: ReviewerAddedEventDetail = {
      reviewer: this.selectedReviewer,
      type: this.type,
    };
    this.reviewer = this.selectedReviewer;
    this.selectedReviewer = '';
    fire(this, 'reviewer-added', detail);
  }
}
