import { Component, input, output } from '@angular/core';
import { Callout } from './callout';
import { Icon } from './icon';

/**
 * The ruled entry band.
 *
 * A manual's form is a line ruled into the page with blanks to fill, so a record
 * is not edited in a dialog floating over the register — the row unfolds in
 * place and the fields are printed into the same grid. Creating opens the same
 * band at the head of the table as a blank line, which is how a new entry is
 * written into a paper register.
 *
 * The world forbids floating panels outright, so this is also the only editing
 * surface in the console: there are no modals, and confirmations resolve inside
 * the band rather than in a dialog over it.
 *
 * The band prints the endpoint it will call, the way the procedure plate already
 * prints the endpoint behind each lifecycle step. An operator who can see that
 * `POST /vehicles` is about to run knows exactly what this form is.
 */
@Component({
  selector: 'app-entry-band',
  imports: [Callout, Icon],
  template: `
    <div class="band">
      <div class="band__head">
        <app-icon name="chevron-down" [size]="13" />
        <span class="band__heading">{{ heading() }}</span>
        @if (endpoint(); as e) {
          <span class="band__endpoint token">{{ e }}</span>
        }
      </div>

      <div class="band__body">
        <ng-content />
      </div>

      @if (error(); as message) {
        <div class="band__error">
          <app-callout tier="warning" heading="Not saved">{{ message }}</app-callout>
        </div>
      }

      <div class="band__actions">
        <button
          class="btn"
          [class.btn--primary]="tone() !== 'danger'"
          [class.btn--danger]="tone() === 'danger'"
          type="button"
          [disabled]="busy() || !canSave()"
          (click)="save.emit()"
        >
          {{ busy() ? busyLabel() : saveLabel() }}
        </button>
        <button class="btn btn--quiet" type="button" [disabled]="busy()" (click)="cancel.emit()">
          Cancel
        </button>
        @if (hint(); as h) {
          <span class="band__hint">{{ h }}</span>
        }
      </div>
    </div>
  `,
  styleUrl: './entry-band.scss',
})
export class EntryBand {
  readonly heading = input.required<string>();
  /** The call this band performs, printed like the procedure plate's endpoints. */
  readonly endpoint = input<string>();
  readonly busy = input(false);
  readonly error = input<string | null>(null);
  readonly saveLabel = input('Save entry');
  readonly busyLabel = input('Saving…');
  readonly canSave = input(true);
  /** `danger` for a band whose action deactivates or terminates a record. */
  readonly tone = input<'default' | 'danger'>('default');
  readonly hint = input<string>();

  readonly save = output<void>();
  readonly cancel = output<void>();
}
