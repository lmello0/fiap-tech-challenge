import { Component, computed, input } from '@angular/core';
import type { CalloutTier } from '../../core/domain/lifecycle';

/**
 * The manual's three severity tiers, and only three.
 *
 * WARNING is for what blocks, destroys or cannot be undone. CAUTION is for
 * what needs attention but is reversible. NOTE is reference. The tier word is
 * printed, not implied by colour alone.
 */
@Component({
  selector: 'app-callout',
  template: `
    <div class="callout" [class]="'callout--' + tier()">
      <p class="callout__tier">{{ word() }}</p>
      <div class="callout__body"><ng-content /></div>
    </div>
  `,
})
export class Callout {
  readonly tier = input<CalloutTier>('note');
  protected readonly word = computed(() => this.tier().toUpperCase());
}
