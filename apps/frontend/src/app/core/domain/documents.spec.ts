import { describe, expect, it } from 'vitest';
import { isValidCnpj, isValidCpf, isValidPlate, formatDocument } from './documents';

describe('document check digits', () => {
  it('accepts real CPFs, formatted or bare', () => {
    expect(isValidCpf('52998224725')).toBe(true);
    expect(isValidCpf('529.982.247-25')).toBe(true);
  });

  it('rejects bad check digits and repeated digits', () => {
    expect(isValidCpf('52998224726')).toBe(false);
    expect(isValidCpf('11111111111')).toBe(false);
    expect(isValidCpf('38420711560')).toBe(false);
    expect(isValidCpf('123')).toBe(false);
  });

  it('accepts a real CNPJ and rejects a corrupted one', () => {
    expect(isValidCnpj('11222333000181')).toBe(true);
    expect(isValidCnpj('11.222.333/0001-81')).toBe(true);
    expect(isValidCnpj('11222333000182')).toBe(false);
    expect(isValidCnpj('11111111111111')).toBe(false);
  });

  it('formats for display', () => {
    expect(formatDocument('CPF', '52998224725')).toBe('529.982.247-25');
    expect(formatDocument('CNPJ', '11222333000181')).toBe('11.222.333/0001-81');
  });

  it('accepts both plate generations', () => {
    expect(isValidPlate('RJP7A41')).toBe(true);
    expect(isValidPlate('ABC1234')).toBe(true);
    expect(isValidPlate('AB12345')).toBe(false);
  });
});
