import {Component, computed, inject, signal} from '@angular/core';
import {Router, RouterLink, RouterLinkActive, RouterOutlet} from '@angular/router';
import {IamStore} from '../../../../iam/application/iam.store';
import {MatButton, MatIconButton} from '@angular/material/button';
import {MatIcon} from '@angular/material/icon';
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from '@angular/material/sidenav';

const DEFAULT_AVATAR = 'default-avatar.jpg';

@Component({
  imports: [
    MatButton,
    MatIcon,
    MatSidenav,
    MatSidenavContainer,
    MatSidenavContent,
    RouterLinkActive,
    RouterOutlet,
    RouterLink,
    MatIconButton
  ],
  selector: 'app-layout',
  styleUrl: './layout.css',
  templateUrl: './layout.html',
})
export class Layout {
  private router = inject(Router);
  protected store = inject(IamStore);

  protected pfpUrl = signal<string | null>(null);
  private failedUrl = signal<string | null>(null);

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

  options = [
    {link: '/home', label: 'Menú', icon: 'home'},
    {link: '/groups/leader', label: 'Líder', icon: 'assignment_ind'},
    {link: '/groups/member', label: 'Miembro', icon: 'person'}
  ];
}
