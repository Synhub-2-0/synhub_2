import {Component, computed, inject, Input, signal} from '@angular/core';
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from "@angular/material/sidenav";
import {MatButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';
import {Router, RouterLink, RouterLinkActive} from '@angular/router';
import {IamStore} from '../../../../iam/application/iam.store';
import {MatIcon} from '@angular/material/icon';

const DEFAULT_AVATAR = 'default-avatar.jpg';

@Component({
  imports: [
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    MatButton,
    FormsModule,
    MatIcon,
    RouterLinkActive,
    RouterLink
  ],
  selector: 'app-sidenav',
  styleUrl: './sidenav.css',
  templateUrl: './sidenav.html',
})
export class Sidenav {
  private router = inject(Router);
  protected store = inject(IamStore);

  protected pfpUrl = signal<string | null>(null);
  private failedUrl = signal<string | null>(null);

  @Input() options: {
    link: string;
    label: string;
    icon: string;
  }[] = [];

  events = signal<('open!' | 'close!')[]>([]);
  opened = signal(false);

  trackEvent(event: 'open!' | 'close!') {
    this.events.update(events => [...events, event]);
  }

  protected avatarSrc = computed(() => {
    const url = this.pfpUrl();
    return url && url !== this.failedUrl() ? url : DEFAULT_AVATAR;
  });

  protected onAvatarError() {
    const url = this.pfpUrl();
    if (url) this.failedUrl.set(url);
  }

  performSignOut(){
    this.store.signOut(this.router);
  }
}
