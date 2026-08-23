import { Component, computed, input } from '@angular/core';
import type { WorkOrderStatus } from '../../core/domain/enums';
import { stepFor } from '../../core/domain/lifecycle';

type Tone = 'ink' | 'warn' | 'caution' | 'ref' | 'ok';

/** Which tier each status is read at. Waiting on a customer is a caution; a
 *  terminal refusal is a warning; a delivered job is spent and reads quiet. */
const TONE: Record<WorkOrderStatus, Tone> = {
  RECEIVED: 'ink',
  WAITING_DIAGNOSTICS: 'caution',
  IN_DIAGNOSTICS: 'ref',
  BUDGET_IN_DRAFT: 'ref',
  WAITING_APPROVAL: 'caution',
  APPROVED: 'ok',
  REFUSED: 'warn',
  IN_PROGRESS: 'ref',
  FINISHED: 'ok',
  WAITING_PICKUP: 'caution',
  DELIVERED: 'ink',
};

/**
 * A work order's status, set as a printed mark rather than a rounded pill.
 * The tone carries severity; the word carries the meaning, so neither is
 * doing the job alone.
 */
@Component({
  selector: 'app-status-mark',
  template: `<span class="mark" [class]="'mark--' + tone()">{{ label() }}</span>`,
  styles: `
    :host {
      display: inline-flex;
    }

    .mark {
      font-family: var(--face-cond);
      font-size: var(--step--1);
      font-weight: 600;
      letter-spacing: 0.07em;
      text-transform: uppercase;
      padding: 0.1rem 0.4rem;
      border: 1px solid currentColor;
      white-space: nowrap;
    }

    .mark--ink {
      color: var(--ink-2);
    }
    .mark--warn {
      color: var(--warn);
      background: var(--warn-field);
    }
    .mark--caution {
      color: var(--caution-ink);
      background: var(--caution-field);
    }
    .mark--ref {
      color: var(--ref);
      background: var(--ref-field);
    }
    .mark--ok {
      color: var(--ok);
      background: var(--ok-field);
    }
  `,
})
export class StatusMark {
  readonly status = input.required<WorkOrderStatus>();

  protected readonly tone = computed<Tone>(() => TONE[this.status()]);
  protected readonly label = computed(() => stepFor(this.status()).title);
}
