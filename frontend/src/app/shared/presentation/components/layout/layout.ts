import {Component, inject, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {IamStore} from '../../../../iam/application/iam.store';
import {Sidenav} from '../sidenav/sidenav';

@Component({
  imports: [
    RouterOutlet,
    Sidenav
  ],
  selector: 'app-layout',
  styleUrl: './layout.css',
  templateUrl: './layout.html',
})
export class Layout {
  private store = inject(IamStore);

  isSignedIn() {
    return this.store.isSignedIn();
  }

  options = signal([
    {link: '/home', label: 'Menú', icon: 'home'},
    {link: '/groups/leader', label: 'Líder', icon: 'assignment_ind'},
    {link: '/groups/member', label: 'Miembro', icon: 'person'}
  ])
}
