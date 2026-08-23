import { Component, input } from '@angular/core';

/**
 * The manual's icon set. Authored here rather than pulled from a library so
 * every mark shares one geometry: a 24px box, 1.75 stroke, square caps and
 * mitred joins — the weight of a technical drawing, not a UI kit.
 */
const PATHS: Record<string, string> = {
  check: 'M4 12.5 9.5 18 20 6',
  alert: 'M12 4 22 20H2L12 4Z M12 10v5 M12 17.6v.4',
  clock: 'M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18Z M12 7v5.4l3.4 2.1',
  arrow: 'M4 12h15 M13 6l6 6-6 6',
  cross: 'M5 5l14 14 M19 5 5 19',
  plus: 'M12 5v14 M5 12h14',
  minus: 'M5 12h14',
  search: 'M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14Z M16 16l4 4',
  filter: 'M3 5h18 M6 12h12 M10 19h4',
  play: 'M7 4.5 19 12 7 19.5Z',
  stop: 'M6 6h12v12H6Z',
  send: 'M3 12 21 4l-4 17-5-6-9-3Z',
  lock: 'M6 11h12v9H6Z M9 11V8a3 3 0 0 1 6 0v3',
  page: 'M6 3h8l4 4v14H6Z M14 3v4h4',
  wrench: 'M20 6.5a4.5 4.5 0 0 1-6.1 4.2L6 18.5 4.5 17l7.8-7.9A4.5 4.5 0 0 1 17 3.2l-2.6 2.6 1.8 1.8L18.8 5c.8.4 1.2.9 1.2 1.5Z',
  box: 'M3 7.5 12 3l9 4.5v9L12 21l-9-4.5Z M3 7.5 12 12l9-4.5 M12 12v9',
  calendar: 'M4 6h16v15H4Z M4 11h16 M8 3v5 M16 3v5',
  people: 'M8 11a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z M2 21c0-3.6 2.7-6 6-6s6 2.4 6 6 M17 21c0-2.6-.8-4.8-2.2-6.2 M16 4.6a3.5 3.5 0 0 1 0 6.8',
  chevron: 'M9 5l7 7-7 7',
  car: 'M4 16.5h16 M5.5 16.5V19H3.5v-2.5 M20.5 16.5V19h-2v-2.5 M3.5 16.5l1.4-6.2A2 2 0 0 1 6.8 8.7h10.4a2 2 0 0 1 1.9 1.6l1.4 6.2Z M7 13h2 M15 13h2',
};

@Component({
  selector: 'app-icon',
  template: `
    <svg
      viewBox="0 0 24 24"
      [attr.width]="size()"
      [attr.height]="size()"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="square"
      stroke-linejoin="miter"
      aria-hidden="true"
      focusable="false"
    >
      <path [attr.d]="d()" />
    </svg>
  `,
  styles: `
    :host {
      display: inline-flex;
      flex: none;
    }
  `,
})
export class Icon {
  readonly name = input.required<string>();
  readonly size = input(16);

  protected d(): string {
    return PATHS[this.name()] ?? '';
  }
}
