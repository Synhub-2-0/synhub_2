import {Component, inject} from '@angular/core';
import {BaseForm} from '../../../../shared/presentation/components/base-form/base-form';
import {Router} from '@angular/router';
import {IamStore} from '../../../application/iam.store';
import {FormControl, FormGroup, ReactiveFormsModule, Validators} from '@angular/forms';
import {SignInCommand} from '../../../domain/model/sign-in.command';
import {MatError, MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatIcon} from '@angular/material/icon';
import {MatButton} from '@angular/material/button';

@Component({
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatLabel,
    MatIcon,
    MatError,
    MatInput,
    MatButton
  ],
  selector: 'app-sign-in-page',
  styleUrl: './sign-in-page.css',
  templateUrl: './sign-in-page.html',
})
export class SignInPage extends BaseForm {
  private router = inject(Router);
  private store = inject(IamStore);

  form = new FormGroup({
    username: new FormControl('', {nonNullable: true, validators: [Validators.required]}),
    password: new FormControl('', {nonNullable: true, validators: [Validators.required]})
  });

  performSignIn() {
    if (this.form.invalid) return;
    const signInCommand = new SignInCommand({
      username: this.form.value.username!,
      password: this.form.value.password!
    });
    this.store.signIn(signInCommand, this.router);
  }

  routeToSignUp() {
    this.router.navigateByUrl('/auth/sign-up').then();
  }
}
