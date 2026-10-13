import {FormGroup} from '@angular/forms';

export class BaseForm {
  protected isInvalidControl(form: FormGroup, controlName: string): boolean {
    return form.controls[controlName].invalid && form.controls[controlName].touched;
  }

  private errorMessageForControl(controlName: string, errorKey: string, error: any): string {
    switch (errorKey) {
      case 'required': return `${controlName} es requerido.`;
      case 'minlength': return `${controlName} debe tener al menos ${error.requiredLength} caracteres.`;
      case 'maxlength': return `${controlName} debe tener como máximo ${error.requiredLength} caracteres.`;
      case 'email': return `${controlName} no es un correo válido.`;
      default: return `${controlName} es inválido.`;
    }
  }

  protected errorMessagesForControl(form: FormGroup, controlName: string): string {
    const control = form.controls[controlName];
    let errorMessages = "";
    let errors = control.errors;
    if (!errors) return errorMessages;
    Object.keys(errors).forEach((errorKey) =>
      errorMessages += this.errorMessageForControl(controlName, errorKey, errors![errorKey]));
    return errorMessages;
  }
}
