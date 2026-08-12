import { FormControl } from '@angular/forms';
import { cepValidator, formatCep, formatPhone, isValidCep, isValidPhone, phoneValidator } from './contact.validators';

describe('contact validators', () => {
  describe('isValidCep', () => {
    it('accepts exactly eight digits with and without mask', () => {
      expect(isValidCep('01310930')).toBeTrue();
      expect(isValidCep('01310-930')).toBeTrue();
    });

    it('rejects incomplete or oversized values', () => {
      expect(isValidCep('013')).toBeFalse();
      expect(isValidCep('0131093')).toBeFalse();
      expect(isValidCep('013109301')).toBeFalse();
      expect(isValidCep('')).toBeFalse();
      expect(isValidCep(null)).toBeFalse();
    });
  });

  describe('isValidPhone', () => {
    it('accepts landlines with ten digits', () => {
      expect(isValidPhone('1133334444')).toBeTrue();
      expect(isValidPhone('(11) 3333-4444')).toBeTrue();
    });

    it('accepts mobiles with eleven digits', () => {
      expect(isValidPhone('11988887777')).toBeTrue();
      expect(isValidPhone('(11) 98888-7777')).toBeTrue();
    });

    it('rejects incomplete numbers', () => {
      expect(isValidPhone('113333444')).toBeFalse();
      expect(isValidPhone('11')).toBeFalse();
      expect(isValidPhone('119888877771')).toBeFalse();
      expect(isValidPhone('')).toBeFalse();
      expect(isValidPhone(null)).toBeFalse();
    });

    it('rejects an area code starting with zero', () => {
      expect(isValidPhone('0133334444')).toBeFalse();
      expect(isValidPhone('01988887777')).toBeFalse();
    });

    it('rejects eleven digit numbers that do not start with nine after the area code', () => {
      expect(isValidPhone('11388887777')).toBeFalse();
    });
  });

  describe('cepValidator', () => {
    it('accepts an empty value so that required is handled separately', () => {
      expect(cepValidator(new FormControl(''))).toBeNull();
      expect(cepValidator(new FormControl(null))).toBeNull();
    });

    it('returns null for a complete CEP', () => {
      expect(cepValidator(new FormControl('01310-930'))).toBeNull();
    });

    it('returns the cep error for an incomplete CEP', () => {
      expect(cepValidator(new FormControl('013'))).toEqual({ cep: true });
    });
  });

  describe('phoneValidator', () => {
    it('accepts an empty value so that required is handled separately', () => {
      expect(phoneValidator(new FormControl(''))).toBeNull();
      expect(phoneValidator(new FormControl(null))).toBeNull();
    });

    it('returns null for landline and mobile numbers', () => {
      expect(phoneValidator(new FormControl('1133334444'))).toBeNull();
      expect(phoneValidator(new FormControl('11988887777'))).toBeNull();
    });

    it('returns the phone error for an incomplete number', () => {
      expect(phoneValidator(new FormControl('11333344'))).toEqual({ phone: true });
    });
  });

  describe('formatCep and formatPhone', () => {
    it('formats complete values and keeps incomplete ones untouched', () => {
      expect(formatCep('01310930')).toBe('01310-930');
      expect(formatCep('013')).toBe('013');
      expect(formatPhone('1133334444')).toBe('(11) 3333-4444');
      expect(formatPhone('11988887777')).toBe('(11) 98888-7777');
      expect(formatPhone('113')).toBe('113');
      expect(formatPhone(null)).toBe('');
    });
  });
});
