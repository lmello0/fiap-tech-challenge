import { Component, computed, input } from '@angular/core';

/**
 * One ruled blank: its label, the control, and what is wrong with it.
 *
 * Errors print under the field in the warning ink rather than as a floating
 * message, and only once the field has been touched — a form that scolds every
 * blank before it has been filled is arguing with the operator.
 */
@Component({
  selector: 'app-form-field',
  template: `
    <label class="ff">
      <span class="ff__label label">
        {{ label() }}
        @if (optional()) {
          <span class="ff__optional">optional</span>
        }
      </span>
      <ng-content />
      @if (message(); as m) {
        <span class="ff__error">{{ m }}</span>
      } @else if (help(); as h) {
        <span class="ff__help">{{ h }}</span>
      }
    </label>
  `,
  styleUrl: './form-field.scss',
  host: { '[class.ff--wide]': 'wide()', '[class.ff--invalid]': 'message() !== null' },
})
export class FormFieldRow {
  readonly label = input.required<string>();
  readonly help = input<string>();
  readonly optional = input(false);
  readonly wide = input(false);

  /** Signal Forms errors for this field, already filtered by the caller. */
  readonly errors = input<readonly { message?: string }[]>([]);
  readonly touched = input(true);

  protected readonly message = computed(() => {
    if (!this.touched()) return null;
    const first = this.errors()[0];
    if (!first) return null;
    return first.message ?? 'This value is not accepted.';
  });
}
