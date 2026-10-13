import { FormControl, FormGroup, Validators } from '@angular/forms';
import { BaseForm } from './base-form';

/** Expone los helpers protegidos para probarlos. */
class TestForm extends BaseForm {
  invalid(form: FormGroup, name: string) { return this.isInvalidControl(form, name); }
  messages(form: FormGroup, name: string) { return this.errorMessagesForControl(form, name); }
}

describe('BaseForm', () => {
  const helper = new TestForm();
  const form = () => new FormGroup({
    name: new FormControl('', [Validators.required]),
    email: new FormControl('x', [Validators.email]),
    password: new FormControl('Corta12', [Validators.minLength(8)]),
    ok: new FormControl('valor'),
  });

  it('un control inválido solo se marca cuando fue tocado', () => {
    const f = form();
    expect(helper.invalid(f, 'name')).toBe(false);
    f.controls.name.markAsTouched();
    expect(helper.invalid(f, 'name')).toBe(true);
  });

  it('mensajes para required y para otros errores', () => {
    const f = form();
    expect(helper.messages(f, 'name')).toBe('name es requerido.');
    expect(helper.messages(f, 'email')).toBe('email no es un correo válido.');
    expect(helper.messages(f, 'ok')).toBe('');
  });

  it('AUDIT FE-03: mensaje de longitud mínima', () => {
    expect(helper.messages(form(), 'password')).toBe('password debe tener al menos 8 caracteres.');
  });
});
