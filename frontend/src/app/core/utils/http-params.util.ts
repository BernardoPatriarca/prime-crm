import { HttpParams } from '@angular/common/http';

export type HttpParamValue = string | number | boolean | null | undefined;

export function buildHttpParams(params: Record<string, HttpParamValue>): HttpParams {
  let httpParams = new HttpParams();
  for (const key of Object.keys(params)) {
    const value = params[key];
    if (value !== null && value !== undefined && value !== '') {
      httpParams = httpParams.set(key, value);
    }
  }
  return httpParams;
}
