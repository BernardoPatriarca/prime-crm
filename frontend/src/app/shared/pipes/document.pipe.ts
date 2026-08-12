import { Pipe, PipeTransform } from '@angular/core';
import { formatDocument } from '../validators/document.validators';

@Pipe({ name: 'document', standalone: true })
export class DocumentPipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    return formatDocument(value);
  }
}
