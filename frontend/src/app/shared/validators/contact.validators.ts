import { AbstractControl, ValidationErrors } from '@angular/forms';
import { onlyDigits } from './document.validators';

const CEP_LENGTH = 8;
const LANDLINE_LENGTH = 10;
const MOBILE_LENGTH = 11;

export function isValidCep(value: string | null | undefined): boolean {
  return onlyDigits(value).length === CEP_LENGTH;
}

export function isValidPhone(value: string | null | undefined): boolean {
  const digits = onlyDigits(value);
  if (digits.length !== LANDLINE_LENGTH && digits.length !== MOBILE_LENGTH) {
    return false;
  }
  if (digits[0] === '0') {
    return false;
  }
  return digits.length !== MOBILE_LENGTH || digits[2] === '9';
}

export function cepValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  return isValidCep(value) ? null : { cep: true };
}

export function phoneValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  return isValidPhone(value) ? null : { phone: true };
}

export function formatCep(value: string | null | undefined): string {
  const digits = onlyDigits(value);
  if (digits.length !== CEP_LENGTH) {
    return value ?? '';
  }
  return `${digits.slice(0, 5)}-${digits.slice(5)}`;
}

export function formatPhone(value: string | null | undefined): string {
  const digits = onlyDigits(value);
  if (digits.length === LANDLINE_LENGTH) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  }
  if (digits.length === MOBILE_LENGTH) {
    return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
  }
  return value ?? '';
}
