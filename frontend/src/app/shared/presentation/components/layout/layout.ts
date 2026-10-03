import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { IamStore } from '../../../../iam/application/iam.store';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, MatIconModule],
  templateUrl: './layout.html',
  styleUrl: './layout.css',
})
export class Layout implements OnInit {
  readonly iamStore = inject(IamStore);
  private readonly router = inject(Router);
  readonly sidebarOpen = signal(true);
  readonly isAuthRoute = signal(true);

  ngOnInit(): void {
    this.checkRoute(this.router.url);
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(event => this.checkRoute(event.urlAfterRedirects));
  }

  private checkRoute(url: string): void {
    this.isAuthRoute.set(url.startsWith('/auth') || url === '/');
  }

  toggleSidebar(): void {
    this.sidebarOpen.update(value => !value);
  }

  get userInitial(): string {
    const name = this.iamStore.currentProfile()?.name || this.iamStore.currentUsername() || 'S';
    return name.charAt(0).toUpperCase();
  }

  onSignOut(): void {
    this.iamStore.signOut(this.router);
  }
}
