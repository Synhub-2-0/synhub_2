import {Component, inject, signal} from '@angular/core';
import {MatSidenav, MatSidenavContainer, MatSidenavContent} from "@angular/material/sidenav";
import {MatButton} from '@angular/material/button';
import {FormsModule} from '@angular/forms';import {Router} from '@angular/router';
import {IamStore} from '../../../../iam/application/iam.store';
import {MatIcon} from '@angular/material/icon';
import {MatPrefix} from '@angular/material/input';

@Component({
  imports: [
    MatSidenavContainer,
    MatSidenav,
    MatSidenavContent,
    MatButton,
    FormsModule,
    MatIcon,
    MatPrefix
  ],
  selector: 'app-sidenav',
  styleUrl: './sidenav.css',
  templateUrl: './sidenav.html',
})
export class Sidenav {
  private router = inject(Router);
  protected store = inject(IamStore);

  events = signal<('open!' | 'close!')[]>([]);
  opened = signal(false);

  trackEvent(event: 'open!' | 'close!') {
    this.events.update(events => [...events, event]);
  }

  performSignOut(){
    this.store.signOut(this.router);
  }
}
