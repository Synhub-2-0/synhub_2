import {Component, inject} from '@angular/core';
import {BaseForm} from '../../../../shared/presentation/components/base-form/base-form';
import {Router} from '@angular/router';
import {IamStore} from '../../../application/iam.store';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators
} from '@angular/forms';
import {SignUpCommand} from '../../../domain/model/sign-up.command';
import {MatError, MatFormField, MatInput, MatLabel} from '@angular/material/input';
import {MatIcon} from '@angular/material/icon';

@Component({
  imports: [
    ReactiveFormsModule,
    MatFormField,
    MatInput,
    MatLabel,
    MatIcon,
    MatError
  ],
  selector: 'app-sign-up-page',
  styleUrl: './sign-up-page.css',
  templateUrl: './sign-up-page.html',
})
export class SignUpPage extends BaseForm {
  private router = inject(Router);
  private store = inject(IamStore)

  form = new FormGroup(
    {
      username: new FormControl('', { nonNullable: true, validators: [Validators.required]}),
      name: new FormControl('', { nonNullable: true, validators: [Validators.required]}),
      surname: new FormControl('', { nonNullable: true, validators: [Validators.required]}),
      email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email]}),
      imgUrl: new FormControl('', { validators: [Validators.pattern('https?://.+')]}),
      password: new FormControl('', { nonNullable: true, validators: [Validators.required]}),
      confirmPassword: new FormControl('', { nonNullable: true, validators: [Validators.required]}),
    },
    {
      validators: passwordMatchValidator
    }
  );

  performSignUp() {
    if (this.form.invalid || this.form.value.password != this.form.value.confirmPassword) return;
    const signUpCommand = new SignUpCommand({
      username: this.form.value.username!,
      name: this.form.value.name!,
      surname: this.form.value.surname!,
      email: this.form.value.email!,
      imgUrl: this.form.value.imgUrl!,
      password: this.form.value.password!,
      accountRoles: ['ROLE_USER']
    });
    this.store.signUp(signUpCommand, this.router);
  }

  routeToSignIn() {
    this.router.navigateByUrl('/auth/sign-in').then();
  }

}

export const passwordMatchValidator: ValidatorFn = (group: AbstractControl)
  : ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  return password === confirmPassword ? null : { passwordMismatch: true };
};
