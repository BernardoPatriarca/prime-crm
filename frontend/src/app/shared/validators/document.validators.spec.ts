import { FormControl } from '@angular/forms';
import {
  cnpjValidator,
  cpfValidator,
  formatDocument,
  isAlphanumericCnpj,
  isValidCnpj,
  isValidCpf,
  onlyDigits
} from './document.validators';

describe('document validators', () => {
  describe('isValidCpf', () => {
    it('accepts valid CPFs with and without mask', () => {
      expect(isValidCpf('11144477735')).toBeTrue();
      expect(isValidCpf('111.444.777-35')).toBeTrue();
      expect(isValidCpf('529.982.247-25')).toBeTrue();
      expect(isValidCpf('529982247-25')).toBeTrue();
    });

    it('rejects CPFs with a wrong check digit', () => {
      expect(isValidCpf('11144477734')).toBeFalse();
      expect(isValidCpf('11144477725')).toBeFalse();
      expect(isValidCpf('529.982.247-26')).toBeFalse();
      expect(isValidCpf('529.982.247-15')).toBeFalse();
    });

    it('rejects repeated digits, wrong length and empty values', () => {
      expect(isValidCpf('00000000000')).toBeFalse();
      expect(isValidCpf('11111111111')).toBeFalse();
      expect(isValidCpf('99999999999')).toBeFalse();
      expect(isValidCpf('1114447773')).toBeFalse();
      expect(isValidCpf('111444777351')).toBeFalse();
      expect(isValidCpf('')).toBeFalse();
      expect(isValidCpf(null)).toBeFalse();
      expect(isValidCpf(undefined)).toBeFalse();
      expect(isValidCpf('abcdefghijk')).toBeFalse();
    });
  });

  describe('isValidCnpj', () => {
    it('accepts valid CNPJs with and without mask', () => {
      expect(isValidCnpj('11222333000181')).toBeTrue();
      expect(isValidCnpj('11.222.333/0001-81')).toBeTrue();
      expect(isValidCnpj('04252011000110')).toBeTrue();
      expect(isValidCnpj('04.252.011/0001-10')).toBeTrue();
    });

    it('rejects CNPJs with a wrong check digit', () => {
      expect(isValidCnpj('11222333000180')).toBeFalse();
      expect(isValidCnpj('11222333000191')).toBeFalse();
      expect(isValidCnpj('04.252.011/0001-11')).toBeFalse();
      expect(isValidCnpj('04.252.011/0001-20')).toBeFalse();
    });

    it('rejects repeated digits, wrong length and empty values', () => {
      expect(isValidCnpj('00000000000000')).toBeFalse();
      expect(isValidCnpj('11111111111111')).toBeFalse();
      expect(isValidCnpj('1122233300018')).toBeFalse();
      expect(isValidCnpj('112223330001811')).toBeFalse();
      expect(isValidCnpj('')).toBeFalse();
      expect(isValidCnpj(null)).toBeFalse();
      expect(isValidCnpj(undefined)).toBeFalse();
    });

    it('accepts alphanumeric CNPJs with and without mask', () => {
      expect(isValidCnpj('12ABC34501DE35')).toBeTrue();
      expect(isValidCnpj('12.ABC.345/01DE-35')).toBeTrue();
      expect(isValidCnpj('12abc34501de35')).toBeTrue();
      expect(isValidCnpj('A1B2C3D4E5F668')).toBeTrue();
    });

    it('rejects alphanumeric CNPJs with a wrong check digit', () => {
      expect(isValidCnpj('12ABC34501DE34')).toBeFalse();
      expect(isValidCnpj('12ABC34501DE45')).toBeFalse();
      expect(isValidCnpj('12.ABC.345/01DE-00')).toBeFalse();
    });

    it('rejects letters in the check digit positions and repeated characters', () => {
      expect(isValidCnpj('12ABC34501DEA5')).toBeFalse();
      expect(isValidCnpj('12ABC34501DE3E')).toBeFalse();
      expect(isValidCnpj('AAAAAAAAAAAAAA')).toBeFalse();
      expect(isValidCnpj('12ABC34501DE3')).toBeFalse();
    });

    it('does not accept a valid CPF as a CNPJ', () => {
      expect(isValidCnpj('11144477735')).toBeFalse();
      expect(isValidCpf('11222333000181')).toBeFalse();
    });
  });

  describe('cpfValidator', () => {
    it('returns null for empty values so that required is handled separately', () => {
      expect(cpfValidator(new FormControl(''))).toBeNull();
      expect(cpfValidator(new FormControl(null))).toBeNull();
    });

    it('returns null for a valid CPF', () => {
      expect(cpfValidator(new FormControl('111.444.777-35'))).toBeNull();
    });

    it('returns the cpf error for an invalid CPF', () => {
      expect(cpfValidator(new FormControl('111.444.777-34'))).toEqual({ cpf: true });
      expect(cpfValidator(new FormControl('12345678900'))).toEqual({ cpf: true });
    });
  });

  describe('cnpjValidator', () => {
    it('returns null for empty values so that required is handled separately', () => {
      expect(cnpjValidator(new FormControl(''))).toBeNull();
      expect(cnpjValidator(new FormControl(null))).toBeNull();
    });

    it('returns null for a valid CNPJ', () => {
      expect(cnpjValidator(new FormControl('11.222.333/0001-81'))).toBeNull();
    });

    it('returns the cnpj error for an invalid CNPJ', () => {
      expect(cnpjValidator(new FormControl('11.222.333/0001-80'))).toEqual({ cnpj: true });
      expect(cnpjValidator(new FormControl('12345678000100'))).toEqual({ cnpj: true });
      expect(cnpjValidator(new FormControl('12.ABC.345/01DE-34'))).toEqual({ cnpj: true });
    });

    it('returns null for a valid alphanumeric CNPJ', () => {
      expect(cnpjValidator(new FormControl('12.ABC.345/01DE-35'))).toBeNull();
    });
  });

  describe('isAlphanumericCnpj', () => {
    it('detects only fourteen position documents carrying letters', () => {
      expect(isAlphanumericCnpj('12ABC34501DE35')).toBeTrue();
      expect(isAlphanumericCnpj('12.abc.345/01de-35')).toBeTrue();
      expect(isAlphanumericCnpj('11222333000181')).toBeFalse();
      expect(isAlphanumericCnpj('111.444.777-35')).toBeFalse();
      expect(isAlphanumericCnpj(null)).toBeFalse();
    });
  });

  describe('onlyDigits', () => {
    it('strips every non numeric character', () => {
      expect(onlyDigits('111.444.777-35')).toBe('11144477735');
      expect(onlyDigits(null)).toBe('');
    });
  });

  describe('formatDocument', () => {
    it('formats CPF and CNPJ according to their length', () => {
      expect(formatDocument('11144477735')).toBe('111.444.777-35');
      expect(formatDocument('11222333000181')).toBe('11.222.333/0001-81');
    });

    it('formats alphanumeric CNPJ in upper case', () => {
      expect(formatDocument('12ABC34501DE35')).toBe('12.ABC.345/01DE-35');
      expect(formatDocument('12abc34501de35')).toBe('12.ABC.345/01DE-35');
    });

    it('returns the original value when the length does not match a document', () => {
      expect(formatDocument('123')).toBe('123');
      expect(formatDocument('')).toBe('');
      expect(formatDocument(null)).toBe('');
    });
  });
});
