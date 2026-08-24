import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Session, type Facet } from '../../core/auth/session';
import { homeFor } from '../../core/auth/landing';
import { safeReturnUrl } from '../sign-in/sign-in';

/**
 * Which volume are you here for?
 *
 * Only ever shown to an account holding both an active Customer facet and an
 * untermininated Worker one — the shop's own bootstrap account, the mechanic
 * who also gets their own car serviced here. A single-facet account never sees
 * this screen: offering one choice is asking someone to confirm a fact.
 *
 * The two options are not a mode toggle on one product. They are two different
 * principals as far as the API is concerned — a CUSTOMER token is refused by
 * every staff endpoint and vice versa — so the screen presents them as what
 * they are: two bound volumes of the same shop's manual, with the choice
 * remembered afterwards and reversible from either masthead.
 */
@Component({
  selector: 'app-choose-facet',
  templateUrl: './choose-facet.html',
  styleUrl: './choose-facet.scss',
})
export class ChooseFacet {
  protected readonly session = inject(Session);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);

  private readonly next = safeReturnUrl(this.route.snapshot.queryParamMap.get('next'));

  protected readonly opening = signal<Facet | null>(null);

  protected async open(facet: Facet): Promise<void> {
    if (this.opening()) return;
    this.opening.set(facet);
    try {
      await this.session.activate(facet);
      // The remembered destination only survives if the facet just chosen can
      // actually open it; otherwise the volume's own front page is correct.
      const wanted = this.next;
      const belongs =
        wanted !== null &&
        (facet === 'customer') === (wanted === '/my' || wanted.startsWith('/my/'));
      await this.router.navigateByUrl(
        belongs ? wanted! : homeFor(facet, this.session.role()),
      );
    } finally {
      this.opening.set(null);
    }
  }
}
