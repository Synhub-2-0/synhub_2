import {Component, inject} from '@angular/core';
import {Router} from '@angular/router';
import {IamStore} from '../../../application/iam.store';
import {MatButton} from '@angular/material/button';

@Component({
  imports: [
    MatButton
  ],
  selector: 'app-authentication-section',
  styleUrl: './authentication-section.css',
  templateUrl: './authentication-section.html',
})
export class AuthenticationSection {
  private router = inject(Router);
  protected store = inject(IamStore);

  performSignIn(){
    this.router.navigate(['/sign-in']).then();
  }

  performSignUp(){
    this.router.navigate(['/sign-up']).then();
  }

  performSignOut(){
    this.store.signOut(this.router);
  }
}
