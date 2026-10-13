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
    expect(helper.messages(f, 'email')).toBe('email es inválido.');
    expect(helper.messages(f, 'ok')).toBe('');
  });
});
