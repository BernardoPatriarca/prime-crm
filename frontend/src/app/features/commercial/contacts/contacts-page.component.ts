import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { DialogModule } from 'primeng/dialog';
import { InputMaskModule } from 'primeng/inputmask';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { Subject, debounceTime, switchMap } from 'rxjs';
import { Contact, ContactRequest } from '../../../core/models/contact.model';
import { Customer } from '../../../core/models/customer.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import { ContactService } from '../../../core/services/contact.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { phoneValidator } from '../../../shared/validators/contact.validators';

const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;
const CUSTOMER_OPTIONS_SIZE = 50;

function toSort(query: TableQuery): string | undefined {
  if (!query.sortField) {
    return undefined;
  }
  return `${query.sortField},${query.sortOrder === -1 ? 'desc' : 'asc'}`;
}

function trimmedOrNull(value: string | null): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed.length > 0 ? trimmed : null;
}

function toIsoDate(date: Date | null): string | null {
  if (!date) {
    return null;
  }
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

function fromIsoDate(iso: string | null): Date | null {
  if (!iso) {
    return null;
  }
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

interface CustomerOption {
  id: string;
  name: string;
  code: string;
}

@Component({
  selector: 'app-contacts-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputMaskModule,
    TextareaModule,
    SelectModule,
    DatePickerModule,
    CheckboxModule,
    TagModule,
    ToggleSwitchModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './contacts-page.component.html',
  styleUrl: './contacts-page.component.scss'
})
export class ContactsPageComponent {
  private readonly contactService = inject(ContactService);
  private readonly customerService = inject(CustomerService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);

  protected readonly contacts = signal<Contact[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly customerFilter = signal<string | null>(null);
  protected readonly customerOptions = signal<CustomerOption[]>([]);
  protected readonly departments = signal<DomainValue[]>([]);

  protected readonly dialogVisible = signal(false);
  protected readonly editingContact = signal<Contact | null>(null);
  protected readonly saving = signal(false);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('CONTATOS_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('CONTATOS_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('CONTATOS_DELETE'));

  private lastQuery: TableQuery = { page: 0, size: 10 };
  private readonly customerSearch = new Subject<string>();

  protected readonly form = this.formBuilder.nonNullable.group({
    customerId: [null as string | null, [Validators.required]],
    name: ['', [Validators.required, Validators.maxLength(200)]],
    positionTitle: ['', [Validators.maxLength(120)]],
    departmentId: [null as string | null],
    email: ['', [Validators.email, Validators.maxLength(180)]],
    phone: ['', [Validators.maxLength(30), phoneValidator]],
    mobile: ['', [Validators.maxLength(30), phoneValidator]],
    birthDate: [null as Date | null],
    linkedin: ['', [Validators.maxLength(255)]],
    primaryContact: [false],
    decisionMaker: [false],
    notes: [''],
    active: [true]
  });

  constructor() {
    this.customerSearch
      .pipe(
        debounceTime(CUSTOMER_SEARCH_DEBOUNCE_MS),
        switchMap((search) => this.customerService.list({ search: search || undefined, size: CUSTOMER_OPTIONS_SIZE, sort: 'name,asc' })),
        takeUntilDestroyed()
      )
      .subscribe((response) => this.customerOptions.set(response.content.map(toCustomerOption)));

    this.load();
    this.loadCustomerOptions();
    this.domainValueService
      .list({ type: 'DEPARTMENT', active: true, size: 200, sort: 'displayOrder,asc' })
      .subscribe((response) => this.departments.set(response.content));
  }

  private loadCustomerOptions(): void {
    this.customerService
      .list({ size: CUSTOMER_OPTIONS_SIZE, sort: 'name,asc' })
      .subscribe((response) => this.customerOptions.set(response.content.map(toCustomerOption)));
  }

  protected onCustomerFilter(search: string): void {
    this.customerSearch.next(search);
  }

  protected onCustomerFilterChange(value: string | null): void {
    this.customerFilter.set(value);
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.contactService
      .list({
        customerId: this.customerFilter() ?? undefined,
        search: this.lastQuery.search,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.contacts.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingContact.set(null);
    this.form.reset({
      customerId: this.customerFilter(),
      name: '',
      positionTitle: '',
      departmentId: null,
      email: '',
      phone: '',
      mobile: '',
      birthDate: null,
      linkedin: '',
      primaryContact: false,
      decisionMaker: false,
      notes: '',
      active: true
    });
    this.dialogVisible.set(true);
  }

  protected openEditDialog(contact: Contact): void {
    this.editingContact.set(contact);
    this.ensureCustomerOption(contact);
    this.form.reset({
      customerId: contact.customer?.id ?? null,
      name: contact.name,
      positionTitle: contact.positionTitle ?? '',
      departmentId: contact.department?.id ?? null,
      email: contact.email ?? '',
      phone: contact.phone ?? '',
      mobile: contact.mobile ?? '',
      birthDate: fromIsoDate(contact.birthDate),
      linkedin: contact.linkedin ?? '',
      primaryContact: contact.primaryContact,
      decisionMaker: contact.decisionMaker,
      notes: contact.notes ?? '',
      active: contact.active
    });
    this.dialogVisible.set(true);
  }

  private ensureCustomerOption(contact: Contact): void {
    const customer = contact.customer;
    if (!customer) {
      return;
    }
    const options = this.customerOptions();
    if (options.some((option) => option.id === customer.id)) {
      return;
    }
    this.customerOptions.set([{ id: customer.id, name: customer.name, code: customer.code }, ...options]);
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const raw = this.form.getRawValue();
    const request: ContactRequest = {
      customerId: raw.customerId as string,
      name: raw.name.trim(),
      positionTitle: trimmedOrNull(raw.positionTitle),
      departmentId: raw.departmentId,
      email: trimmedOrNull(raw.email),
      phone: trimmedOrNull(raw.phone),
      mobile: trimmedOrNull(raw.mobile),
      birthDate: toIsoDate(raw.birthDate),
      linkedin: trimmedOrNull(raw.linkedin),
      primaryContact: raw.primaryContact,
      decisionMaker: raw.decisionMaker,
      notes: trimmedOrNull(raw.notes),
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingContact();
    const request$ = editing ? this.contactService.update(editing.id, request) : this.contactService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'commercialPages.contacts.messages.updated' : 'commercialPages.contacts.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(contact: Contact): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: contact.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.contactService.delete(contact.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('commercialPages.contacts.messages.deleted')
          });
          this.load();
        });
      }
    });
  }
}

function toCustomerOption(customer: Customer): CustomerOption {
  return { id: customer.id, name: customer.name, code: customer.code };
}
