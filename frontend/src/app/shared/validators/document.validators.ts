import { AbstractControl, ValidationErrors } from '@angular/forms';

const CPF_LENGTH = 11;
const CNPJ_LENGTH = 14;
const CNPJ_FIRST_WEIGHTS = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
const CNPJ_SECOND_WEIGHTS = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

const CNPJ_ALPHANUMERIC_PATTERN = /^[A-Z0-9]{12}[0-9]{2}$/;
const ASCII_ZERO = 48;

export function onlyDigits(value: string | null | undefined): string {
  return (value ?? '').replace(/\D/g, '');
}

export function onlyAlphanumeric(value: string | null | undefined): string {
  return (value ?? '').toUpperCase().replace(/[^A-Z0-9]/g, '');
}

function allCharactersEqual(value: string): boolean {
  return value.split('').every((character) => character === value[0]);
}

export function isValidCpf(value: string | null | undefined): boolean {
  const digits = onlyDigits(value);
  if (digits.length !== CPF_LENGTH || allCharactersEqual(digits)) {
    return false;
  }

  const numbers = digits.split('').map(Number);

  let sum = 0;
  for (let index = 0; index < 9; index += 1) {
    sum += numbers[index] * (10 - index);
  }
  let firstCheckDigit = (sum * 10) % 11;
  if (firstCheckDigit === 10) {
    firstCheckDigit = 0;
  }
  if (firstCheckDigit !== numbers[9]) {
    return false;
  }

  sum = 0;
  for (let index = 0; index < 10; index += 1) {
    sum += numbers[index] * (11 - index);
  }
  let secondCheckDigit = (sum * 10) % 11;
  if (secondCheckDigit === 10) {
    secondCheckDigit = 0;
  }
  return secondCheckDigit === numbers[10];
}

export function isAlphanumericCnpj(value: string | null | undefined): boolean {
  const characters = onlyAlphanumeric(value);
  return characters.length === CNPJ_LENGTH && /[A-Z]/.test(characters.slice(0, 12));
}

export function isValidCnpj(value: string | null | undefined): boolean {
  const characters = onlyAlphanumeric(value);
  if (characters.length !== CNPJ_LENGTH || allCharactersEqual(characters)) {
    return false;
  }
  if (!CNPJ_ALPHANUMERIC_PATTERN.test(characters)) {
    return false;
  }

  const values = characters.split('').map((character) => character.charCodeAt(0) - ASCII_ZERO);
  const checkDigitFor = (weights: number[]): number => {
    let sum = 0;
    for (let index = 0; index < weights.length; index += 1) {
      sum += values[index] * weights[index];
    }
    const remainder = sum % 11;
    return remainder < 2 ? 0 : 11 - remainder;
  };

  return checkDigitFor(CNPJ_FIRST_WEIGHTS) === values[12] && checkDigitFor(CNPJ_SECOND_WEIGHTS) === values[13];
}

export function cpfValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  return isValidCpf(value) ? null : { cpf: true };
}

export function cnpjValidator(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string | null;
  if (!value) {
    return null;
  }
  return isValidCnpj(value) ? null : { cnpj: true };
}

export function formatDocument(value: string | null | undefined): string {
  const characters = onlyAlphanumeric(value);
  if (characters.length === CNPJ_LENGTH) {
    return `${characters.slice(0, 2)}.${characters.slice(2, 5)}.${characters.slice(5, 8)}/${characters.slice(8, 12)}-${characters.slice(12)}`;
  }
  const digits = onlyDigits(value);
  if (digits.length === CPF_LENGTH) {
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
  }
  return value ?? '';
}
