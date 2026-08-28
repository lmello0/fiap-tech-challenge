import { Component, computed, input, signal } from '@angular/core';
import type { WorkerRole, WorkOrderStatus } from '../../core/domain/enums';
import { WORKER_ROLE_LABEL } from '../../core/domain/enums';
import { LIFECYCLE, type LifecycleStep, stepFor, stepIndex } from '../../core/domain/lifecycle';

/**
 * THE STEP RAIL — this console's signature.
 *
 * A work order's whole life as a numbered procedure, the way a service manual
 * prints one. Every tick is a step; the marked tick is where the job stands.
 * Focusing or hovering a tick states that step's precondition and which roles
 * the backend will actually let perform it — which is what turns an invisible
 * permission model into something a person can read off the page.
 *
 * `compact` is the board-row rail: ticks only, no panel, the whole procedure
 * legible in one glance across twenty rows.
 */
@Component({
  selector: 'app-step-rail',
  imports: [],
  template: `
    <div class="rail" [class.rail--compact]="compact()">
      <ol class="rail__ticks">
        @for (step of steps(); track step.n) {
          <li class="rail__tick-wrap">
            <button
              type="button"
              class="rail__tick"
              [class.is-done]="step.n < here()"
              [class.is-here]="step.n === here() && !refused()"
              [class.is-blocked]="step.n === here() && blocked()"
              [attr.aria-current]="step.n === here() ? 'step' : null"
              [attr.aria-label]="'Step ' + step.n + ', ' + step.title + '. ' + step.precondition"
              (mouseenter)="peek.set(step)"
              (mouseleave)="peek.set(null)"
              (focus)="peek.set(step)"
              (blur)="peek.set(null)"
            >
              <span class="rail__n">{{ step.n }}</span>
            </button>
          </li>
        }
        @if (branch(); as bs) {
          <li class="rail__tick-wrap rail__tick-wrap--branch">
            <button
              type="button"
              class="rail__tick"
              [class.is-refused]="bs.status === 'REFUSED'"
              [class.is-cancelled]="bs.status === 'CANCELLED'"
              aria-current="step"
              [attr.aria-label]="bs.title + '. ' + bs.precondition"
              (mouseenter)="peek.set(bs)"
              (mouseleave)="peek.set(null)"
              (focus)="peek.set(bs)"
              (blur)="peek.set(null)"
            >
              <span class="rail__n">{{ bs.status === 'REFUSED' ? 'R' : 'C' }}</span>
            </button>
          </li>
        }
      </ol>

      @if (!compact()) {
        <div class="rail__panel" role="status">
          @let s = peek() ?? current();
          <p class="rail__panel-title">
            <span class="label rail__panel-n">{{
              s.status === 'REFUSED' || s.status === 'CANCELLED' ? 'Branch' : 'Step ' + s.n
            }}</span>
            {{ s.title }}
          </p>
          <p class="rail__panel-pre">{{ s.precondition }}</p>
          @if (s.action; as a) {
            <p class="rail__panel-roles">
              <span class="label">Performed by</span>
              {{ roleList(a.roles) }}
              <span class="token rail__panel-ep">{{ a.endpoint }}</span>
            </p>
          } @else {
            <p class="rail__panel-roles">
              <span class="label">Performed by</span>
              {{
                s.status === 'APPROVED' || s.status === 'REFUSED'
                  ? 'the customer — no staff action'
                  : s.status === 'CANCELLED'
                    ? 'an attendant or manager'
                    : '—'
              }}
            </p>
          }
        </div>
      }
    </div>
  `,
  styles: `
    :host {
      display: block;
    }

    .rail__ticks {
      display: flex;
      align-items: center;
      gap: 0;
      list-style: none;
      padding: 0;
      margin: 0;
    }

    .rail__tick-wrap {
      display: flex;
      align-items: center;
    }

    /* the rule that joins the ticks — the procedure's spine */
    .rail__tick-wrap + .rail__tick-wrap::before {
      content: '';
      display: block;
      width: 0.55rem;
      height: 1px;
      background: var(--rule-strong);
    }

    .rail__tick-wrap--branch::before {
      background: var(--warn) !important;
    }

    .rail__tick {
      display: grid;
      place-items: center;
      width: 1.35rem;
      height: 1.35rem;
      padding: 0;
      border: 1px solid var(--rule-strong);
      background: var(--plate);
      color: var(--ink-3);
      font-family: var(--face-mono);
      font-size: 0.62rem;
      font-weight: 500;
      cursor: pointer;
      /* the stamp: a crisp mechanical change, no float, no bounce */
      transition:
        background-color 90ms linear,
        border-color 90ms linear,
        color 90ms linear;
    }

    .rail__tick:hover {
      border-color: var(--ink);
      color: var(--ink);
    }

    /* Done is filled in press ink at a lighter value, not in a spot colour:
       the world's four spots are severities, and a completed step is not one.
       Filled-versus-hollow carries the state; value carries the rank. */
    .rail__tick.is-done {
      background: var(--ink-2);
      border-color: var(--ink-2);
      color: var(--ink-inv);
    }

    .rail__tick.is-here {
      background: var(--ink);
      border-color: var(--ink);
      color: var(--ink-inv);
      /* the inspector's stamp: a ruled box drawn round the current step */
      outline: 1px solid var(--ink);
      outline-offset: 2px;
    }

    .rail__tick.is-here.is-blocked {
      background: var(--warn);
      border-color: var(--warn);
      outline-color: var(--warn);
    }

    .rail__tick.is-refused,
    .rail__tick.is-cancelled {
      background: var(--warn);
      border-color: var(--warn);
      color: var(--ink-inv);
      outline: 1px solid var(--warn);
      outline-offset: 2px;
    }

    .rail--compact .rail__tick {
      width: 0.95rem;
      height: 0.95rem;
      font-size: 0.52rem;
    }

    .rail--compact .rail__tick-wrap + .rail__tick-wrap::before {
      width: 0.22rem;
    }

    /* At eighteen rows the numerals are 180 glyphs nobody reads. The eye wants
       filled-bar length first, so only the current tick keeps its number; the
       rest stay addressable through their aria-label. */
    .rail--compact .rail__n {
      visibility: hidden;
    }

    .rail--compact .rail__tick.is-here .rail__n,
    .rail--compact .rail__tick.is-refused .rail__n,
    .rail--compact .rail__tick.is-cancelled .rail__n {
      visibility: visible;
    }

    /* --- the panel ------------------------------------------------------- */

    .rail__panel {
      margin-top: 0.85rem;
      border-top: 1px solid var(--rule);
      padding-top: 0.7rem;
      max-width: 68ch;
    }

    .rail__panel-title {
      font-family: var(--face-cond);
      font-size: var(--step-1);
      font-weight: 600;
      display: flex;
      align-items: baseline;
      gap: 0.5rem;
    }

    .rail__panel-n {
      color: var(--ink-3);
    }

    .rail__panel-pre {
      margin-top: 0.2rem;
      color: var(--ink-2);
    }

    .rail__panel-roles {
      margin-top: 0.4rem;
      display: flex;
      align-items: baseline;
      gap: 0.5rem;
      flex-wrap: wrap;
      color: var(--ink-2);
    }

    .rail__panel-ep {
      color: var(--ink-3);
    }
  `,
})
export class StepRail {
  readonly status = input.required<WorkOrderStatus>();
  readonly compact = input(false);
  readonly blocked = input(false);

  protected readonly peek = signal<LifecycleStep | null>(null);

  protected readonly steps = computed(() =>
    this.refused() ? LIFECYCLE.filter((s) => s.n <= 5) : LIFECYCLE,
  );

  protected readonly here = computed(() => stepIndex(this.status()));
  protected readonly refused = computed(() => this.status() === 'REFUSED');
  protected readonly current = computed(() => stepFor(this.status()));

  /**
   * The branch tick, when the order sits off the main line. CANCELLED has no
   * fixed step to truncate at — the backend allows it from almost anywhere —
   * so unlike REFUSED it does not shorten `steps()`; it only adds its own tick.
   */
  protected readonly branch = computed<LifecycleStep | null>(() => {
    const status = this.status();
    if (status === 'REFUSED' || status === 'CANCELLED') return stepFor(status);
    return null;
  });

  protected roleList(roles: readonly WorkerRole[]): string {
    return roles.map((r) => WORKER_ROLE_LABEL[r]).join(' or ');
  }
}
