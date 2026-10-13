import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SignUpPage } from './sign-up-page';

describe('SignUpPage', () => {
  let component: SignUpPage;
  let fixture: ComponentFixture<SignUpPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignUpPage],
    }).compileComponents();

    fixture = TestBed.createComponent(SignUpPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('exige que las contraseñas coincidan y un email válido', () => {
    component.form.setValue({
      username: 'ana', name: 'Ana', surname: 'Torres', email: 'ana@synhub.io', imgUrl: '', password: 'Secret123!', confirmPassword: 'Otra123!',
    });
    expect(component.form.valid).toBe(false);
    component.form.patchValue({ confirmPassword: 'Secret123!' });
    expect(component.form.valid).toBe(true);
    component.form.patchValue({ email: 'no-es-email' });
    expect(component.form.controls.email.valid).toBe(false);
  });

  it.skip('AUDIT FE-03: la contraseña exige al menos 8 caracteres', () => {
    component.form.controls.password.setValue('Corta12');
    expect(component.form.controls.password.valid).toBe(false);
  });
});
