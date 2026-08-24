/**
 * CPF and CNPJ, checked here rather than at the API.
 *
 * The backend validates both, but an invalid check digit currently surfaces as a
 * 500 rather than a field error — a mistyped digit would hand the operator
 * "Something went wrong while processing the request" and no way to act on it.
 * Validating the check digits in the form turns that into a message against the
 * field, which is where a typo belongs.
 *
 * Both algorithms are the standard weighted modulus-11: sum each digit against a
 * descending weight, take the remainder, and derive the check digit from it. They
 * are arithmetic, not a guess about the backend's rules.
 */

/** Strip formatting: operators paste `384.207.115-60` as readily as digits. */
export function digitsOnly(value: string): string {
  return value.replace(/\D/g, '');
}

export function isValidCpf(value: string): boolean {
  const d = digitsOnly(value);
  if (d.length !== 11) return false;
  // Eleven of the same digit passes the arithmetic but is never a real CPF.
  if (/^(\d)\1{10}$/.test(d)) return false;

  for (const [length, start] of [[9, 10] as const, [10, 11] as const]) {
    let sum = 0;
    for (let i = 0; i < length; i++) sum += Number(d[i]) * (start - i);
    const check = (sum * 10) % 11 % 10;
    if (check !== Number(d[length])) return false;
  }
  return true;
}

export function isValidCnpj(value: string): boolean {
  const d = digitsOnly(value);
  if (d.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(d)) return false;

  // Weights run 5..2 then 9..2 for the first digit, shifted one place for the second.
  const weights = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  for (const length of [12, 13]) {
    const w = weights.slice(weights.length - length);
    let sum = 0;
    for (let i = 0; i < length; i++) sum += Number(d[i]) * w[i];
    const remainder = sum % 11;
    const check = remainder < 2 ? 0 : 11 - remainder;
    if (check !== Number(d[length])) return false;
  }
  return true;
}

export function isValidDocument(type: 'CPF' | 'CNPJ', value: string): boolean {
  return type === 'CPF' ? isValidCpf(value) : isValidCnpj(value);
}

/** `38420711560` → `384.207.115-60`. Display only; the wire takes bare digits. */
export function formatDocument(type: 'CPF' | 'CNPJ', value: string): string {
  const d = digitsOnly(value);
  if (type === 'CPF' && d.length === 11) {
    return `${d.slice(0, 3)}.${d.slice(3, 6)}.${d.slice(6, 9)}-${d.slice(9)}`;
  }
  if (type === 'CNPJ' && d.length === 14) {
    return `${d.slice(0, 2)}.${d.slice(2, 5)}.${d.slice(5, 8)}/${d.slice(8, 12)}-${d.slice(12)}`;
  }
  return value;
}

/**
 * Mercosul (`ABC1D23`) and the older Brazilian pattern (`ABC1234`).
 * The API caps the field at 7 characters but does not check the shape.
 */
export function isValidPlate(value: string): boolean {
  const p = value.toUpperCase().replace(/[^A-Z0-9]/g, '');
  return /^[A-Z]{3}\d[A-Z0-9]\d{2}$/.test(p);
}

/** `(11) 98214-7730` → `11982147730`. */
export function normalisePhone(value: string): string {
  return digitsOnly(value);
}

export function formatPhone(value: string): string {
  const d = digitsOnly(value);
  if (d.length === 11) return `(${d.slice(0, 2)}) ${d.slice(2, 7)}-${d.slice(7)}`;
  if (d.length === 10) return `(${d.slice(0, 2)}) ${d.slice(2, 6)}-${d.slice(6)}`;
  return value;
}
