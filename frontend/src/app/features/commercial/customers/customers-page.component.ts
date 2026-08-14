import { Component, WritableSignal, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { TranslatePipe, TranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService, SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputMaskModule } from 'primeng/inputmask';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TabsModule } from 'primeng/tabs';
import { TagModule } from 'primeng/tag';
import { TextareaModule } from 'primeng/textarea';
import { ToggleSwitchModule } from 'primeng/toggleswitch';
import { TooltipModule } from 'primeng/tooltip';
import { AdminUser } from '../../../core/models/admin-user.model';
import { Customer, CustomerRequest, PersonType } from '../../../core/models/customer.model';
import { DomainValue } from '../../../core/models/domain-value.model';
import { AdminUserService } from '../../../core/services/admin-user.service';
import { CustomerService } from '../../../core/services/customer.service';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { openCreateDialogFromRoute } from '../../../shared/utils/creation-route.util';
import { SessionStore } from '../../../core/store/session.store';
import { GenericTableComponent, TableQuery } from '../../../shared/components/generic-table/generic-table.component';
import { DocumentPipe } from '../../../shared/pipes/document.pipe';
import { domainChipStyle } from '../../../shared/utils/domain-color.util';
import { cnpjValidator, cpfValidator } from '../../../shared/validators/document.validators';
import { cepValidator, phoneValidator } from '../../../shared/validators/contact.validators';

const PERSON_TYPES: PersonType[] = ['FISICA', 'JURIDICA'];

const BRAZILIAN_STATES = [
  'AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG',
  'PA', 'PB', 'PR', 'PE', 'PI', 'RJ', 'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'
];

const CUSTOMER_TABS = ['general', 'contact', 'address', 'commercial'] as const;

type CustomerTab = (typeof CUSTOMER_TABS)[number];

const TAB_CONTROLS: Record<CustomerTab, string[]> = {
  general: [
    'name', 'tradeName', 'personType', 'document', 'stateRegistration', 'municipalRegistration',
    'clientTypeId', 'segmentId', 'activityBranchId', 'categoryId', 'originId', 'statusId'
  ],
  contact: ['phone', 'mobile', 'email', 'financialEmail', 'website', 'instagram', 'linkedin'],
  address: ['zipCode', 'street', 'number', 'complement', 'district', 'city', 'state', 'country'],
  commercial: ['ownerUserId', 'teamId', 'creditLimit', 'paymentTerms', 'tagIds', 'notes', 'active']
};

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

@Component({
  selector: 'app-customers-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    FormsModule,
    TranslatePipe,
    DocumentPipe,
    GenericTableComponent,
    TableModule,
    ButtonModule,
    DialogModule,
    InputTextModule,
    InputMaskModule,
    InputNumberModule,
    TextareaModule,
    SelectModule,
    MultiSelectModule,
    TabsModule,
    TagModule,
    ToggleSwitchModule,
    TooltipModule,
    SharedModule
  ],
  templateUrl: './customers-page.component.html',
  styleUrl: './customers-page.component.scss'
})
export class CustomersPageComponent {
  private readonly customerService = inject(CustomerService);
  private readonly domainValueService = inject(DomainValueService);
  private readonly adminUserService = inject(AdminUserService);
  private readonly sessionStore = inject(SessionStore);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly translate = inject(TranslateService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);

  protected readonly personTypes = PERSON_TYPES;
  protected readonly states = BRAZILIAN_STATES;
  protected readonly tabs = CUSTOMER_TABS;

  protected readonly lockedPersonType = signal<PersonType | null>(null);
  protected readonly pageKey = signal('customers');

  protected readonly customers = signal<Customer[]>([]);
  protected readonly total = signal(0);
  protected readonly loading = signal(false);

  protected readonly personTypeFilter = signal<PersonType | null>(null);
  protected readonly clientTypeFilter = signal<string | null>(null);
  protected readonly segmentFilter = signal<string | null>(null);
  protected readonly activeFilter = signal<boolean | null>(null);

  protected readonly dialogVisible = signal(false);
  protected readonly editingCustomer = signal<Customer | null>(null);
  protected readonly saving = signal(false);
  protected readonly activeTab = signal<CustomerTab>('general');

  protected readonly clientTypes = signal<DomainValue[]>([]);
  protected readonly segments = signal<DomainValue[]>([]);
  protected readonly activityBranches = signal<DomainValue[]>([]);
  protected readonly categories = signal<DomainValue[]>([]);
  protected readonly origins = signal<DomainValue[]>([]);
  protected readonly statuses = signal<DomainValue[]>([]);
  protected readonly tags = signal<DomainValue[]>([]);
  protected readonly teams = signal<DomainValue[]>([]);
  protected readonly users = signal<AdminUser[]>([]);

  protected readonly canCreate = computed(() => this.sessionStore.hasPermission('CLIENTES_CREATE'));
  protected readonly canEdit = computed(() => this.sessionStore.hasPermission('CLIENTES_EDIT'));
  protected readonly canDelete = computed(() => this.sessionStore.hasPermission('CLIENTES_DELETE'));

  protected readonly titleKey = computed(() => `commercialPages.${this.pageKey()}.title`);
  protected readonly descriptionKey = computed(() => `commercialPages.${this.pageKey()}.description`);
  protected readonly newButtonKey = computed(() => `commercialPages.${this.pageKey()}.newButton`);
  protected readonly emptyStateKey = computed(() => `commercialPages.${this.pageKey()}.emptyState`);

  protected readonly personTypeOptions = computed(() => {
    this.translate.currentLang();
    return PERSON_TYPES.map((value) => ({
      value,
      label: this.translate.instant(`commercialPages.customers.personType.${value}`)
    }));
  });

  protected readonly activeOptions = computed(() => {
    this.translate.currentLang();
    return [
      { value: true, label: this.translate.instant('common.status.active') },
      { value: false, label: this.translate.instant('common.status.inactive') }
    ];
  });

  private lastQuery: TableQuery = { page: 0, size: 10 };

  protected readonly form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(200)]],
    tradeName: ['', [Validators.maxLength(200)]],
    personType: ['JURIDICA' as PersonType, [Validators.required]],
    document: ['', [Validators.required, cnpjValidator]],
    stateRegistration: ['', [Validators.maxLength(30)]],
    municipalRegistration: ['', [Validators.maxLength(30)]],
    clientTypeId: [null as string | null],
    segmentId: [null as string | null],
    activityBranchId: [null as string | null],
    categoryId: [null as string | null],
    originId: [null as string | null],
    statusId: [null as string | null],
    phone: ['', [Validators.maxLength(30), phoneValidator]],
    mobile: ['', [Validators.maxLength(30), phoneValidator]],
    email: ['', [Validators.email, Validators.maxLength(180)]],
    financialEmail: ['', [Validators.email, Validators.maxLength(180)]],
    website: ['', [Validators.maxLength(255)]],
    instagram: ['', [Validators.maxLength(120)]],
    linkedin: ['', [Validators.maxLength(255)]],
    zipCode: ['', [Validators.maxLength(20), cepValidator]],
    street: ['', [Validators.maxLength(200)]],
    number: ['', [Validators.maxLength(20)]],
    complement: ['', [Validators.maxLength(120)]],
    district: ['', [Validators.maxLength(120)]],
    city: ['', [Validators.maxLength(120)]],
    state: [null as string | null],
    country: ['Brasil', [Validators.maxLength(60)]],
    ownerUserId: [null as string | null],
    teamId: [null as string | null],
    creditLimit: [null as number | null],
    paymentTerms: ['', [Validators.maxLength(120)]],
    tagIds: [[] as string[]],
    notes: [''],
    active: [true]
  });

  constructor() {
    const data = this.route.snapshot.data;
    const locked = (data['personType'] as PersonType | undefined) ?? null;
    this.lockedPersonType.set(locked);
    this.pageKey.set((data['pageKey'] as string | undefined) ?? 'customers');

    this.form.controls.personType.valueChanges.pipe(takeUntilDestroyed()).subscribe((personType) => {
      this.applyDocumentValidators(personType);
      this.form.controls.document.updateValueAndValidity();
    });

    this.form.controls.document.valueChanges.pipe(takeUntilDestroyed()).subscribe((document) => {
      const upperCased = (document ?? '').toUpperCase();
      if (document !== upperCased) {
        this.form.controls.document.setValue(upperCased, { emitEvent: false });
      }
    });

    openCreateDialogFromRoute(() => {
      if (this.canCreate()) {
        this.openCreateDialog();
      }
    });

    this.load();
    this.loadDomain('CLIENT_TYPE', this.clientTypes);
    this.loadDomain('MARKET_SEGMENT', this.segments);
    this.loadDomain('ACTIVITY_BRANCH', this.activityBranches);
    this.loadDomain('CATEGORY', this.categories);
    this.loadDomain('LEAD_ORIGIN', this.origins);
    this.loadDomain('GENERIC_STATUS', this.statuses);
    this.loadDomain('TAG', this.tags);
    this.loadDomain('TEAM', this.teams);
    this.adminUserService.list({ size: 200, sort: 'name,asc' }).subscribe((response) => this.users.set(response.content));
  }

  private loadDomain(type: string, target: WritableSignal<DomainValue[]>): void {
    this.domainValueService
      .list({ type, active: true, size: 200, sort: 'displayOrder,asc' })
      .subscribe((response) => target.set(response.content));
  }

  private applyDocumentValidators(personType: PersonType): void {
    this.form.controls.document.setValidators([
      Validators.required,
      personType === 'FISICA' ? cpfValidator : cnpjValidator
    ]);
  }

  protected onTabChange(value: string | number): void {
    this.activeTab.set(value as CustomerTab);
  }

  protected onQueryChange(query: TableQuery): void {
    this.lastQuery = query;
    this.load();
  }

  protected onFilterChange(): void {
    this.lastQuery = { ...this.lastQuery, page: 0 };
    this.load();
  }

  protected clearFilters(): void {
    this.personTypeFilter.set(null);
    this.clientTypeFilter.set(null);
    this.segmentFilter.set(null);
    this.activeFilter.set(null);
    this.onFilterChange();
  }

  private load(): void {
    this.loading.set(true);
    this.customerService
      .list({
        search: this.lastQuery.search,
        personType: this.lockedPersonType() ?? this.personTypeFilter() ?? undefined,
        clientTypeId: this.clientTypeFilter() ?? undefined,
        segmentId: this.segmentFilter() ?? undefined,
        active: this.activeFilter() ?? undefined,
        page: this.lastQuery.page,
        size: this.lastQuery.size,
        sort: toSort(this.lastQuery)
      })
      .subscribe({
        next: (response) => {
          this.customers.set(response.content);
          this.total.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false)
      });
  }

  protected openCreateDialog(): void {
    this.editingCustomer.set(null);
    this.activeTab.set('general');
    const personType = this.lockedPersonType() ?? 'JURIDICA';
    this.form.reset({
      name: '',
      tradeName: '',
      personType,
      document: '',
      stateRegistration: '',
      municipalRegistration: '',
      clientTypeId: null,
      segmentId: null,
      activityBranchId: null,
      categoryId: null,
      originId: null,
      statusId: null,
      phone: '',
      mobile: '',
      email: '',
      financialEmail: '',
      website: '',
      instagram: '',
      linkedin: '',
      zipCode: '',
      street: '',
      number: '',
      complement: '',
      district: '',
      city: '',
      state: null,
      country: 'Brasil',
      ownerUserId: null,
      teamId: null,
      creditLimit: null,
      paymentTerms: '',
      tagIds: [],
      notes: '',
      active: true
    });
    this.applyDocumentValidators(personType);
    this.form.controls.document.updateValueAndValidity();
    this.dialogVisible.set(true);
  }

  protected openEditDialog(customer: Customer): void {
    this.editingCustomer.set(customer);
    this.activeTab.set('general');
    this.form.reset({
      name: customer.name,
      tradeName: customer.tradeName ?? '',
      personType: customer.personType,
      document: customer.document ?? '',
      stateRegistration: customer.stateRegistration ?? '',
      municipalRegistration: customer.municipalRegistration ?? '',
      clientTypeId: customer.clientType?.id ?? null,
      segmentId: customer.segment?.id ?? null,
      activityBranchId: customer.activityBranch?.id ?? null,
      categoryId: customer.category?.id ?? null,
      originId: customer.origin?.id ?? null,
      statusId: customer.status?.id ?? null,
      phone: customer.phone ?? '',
      mobile: customer.mobile ?? '',
      email: customer.email ?? '',
      financialEmail: customer.financialEmail ?? '',
      website: customer.website ?? '',
      instagram: customer.instagram ?? '',
      linkedin: customer.linkedin ?? '',
      zipCode: customer.zipCode ?? '',
      street: customer.street ?? '',
      number: customer.number ?? '',
      complement: customer.complement ?? '',
      district: customer.district ?? '',
      city: customer.city ?? '',
      state: customer.state ?? null,
      country: customer.country ?? 'Brasil',
      ownerUserId: customer.owner?.id ?? null,
      teamId: customer.team?.id ?? null,
      creditLimit: customer.creditLimit ?? null,
      paymentTerms: customer.paymentTerms ?? '',
      tagIds: customer.tags.map((tag) => tag.id),
      notes: customer.notes ?? '',
      active: customer.active
    });
    this.applyDocumentValidators(customer.personType);
    this.form.controls.document.updateValueAndValidity();
    this.dialogVisible.set(true);
  }

  protected closeDialog(): void {
    this.dialogVisible.set(false);
  }

  protected documentMask(): string {
    return this.form.controls.personType.value === 'FISICA' ? '999.999.999-99' : '**.***.***/****-99';
  }

  protected documentLabelKey(): string {
    return this.form.controls.personType.value === 'FISICA'
      ? 'commercialPages.customers.dialog.cpfLabel'
      : 'commercialPages.customers.dialog.cnpjLabel';
  }

  private firstInvalidTab(): CustomerTab | null {
    for (const tab of CUSTOMER_TABS) {
      const hasInvalid = TAB_CONTROLS[tab].some((name) => this.form.get(name)?.invalid === true);
      if (hasInvalid) {
        return tab;
      }
    }
    return null;
  }

  protected save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      const tab = this.firstInvalidTab();
      if (tab) {
        this.activeTab.set(tab);
      }
      return;
    }

    const raw = this.form.getRawValue();
    const request: CustomerRequest = {
      name: raw.name.trim(),
      tradeName: trimmedOrNull(raw.tradeName),
      personType: raw.personType,
      document: trimmedOrNull(raw.document),
      stateRegistration: trimmedOrNull(raw.stateRegistration),
      municipalRegistration: trimmedOrNull(raw.municipalRegistration),
      clientTypeId: raw.clientTypeId,
      segmentId: raw.segmentId,
      activityBranchId: raw.activityBranchId,
      categoryId: raw.categoryId,
      originId: raw.originId,
      statusId: raw.statusId,
      ownerUserId: raw.ownerUserId,
      teamId: raw.teamId,
      phone: trimmedOrNull(raw.phone),
      mobile: trimmedOrNull(raw.mobile),
      email: trimmedOrNull(raw.email),
      financialEmail: trimmedOrNull(raw.financialEmail),
      website: trimmedOrNull(raw.website),
      instagram: trimmedOrNull(raw.instagram),
      linkedin: trimmedOrNull(raw.linkedin),
      zipCode: trimmedOrNull(raw.zipCode),
      street: trimmedOrNull(raw.street),
      number: trimmedOrNull(raw.number),
      complement: trimmedOrNull(raw.complement),
      district: trimmedOrNull(raw.district),
      city: trimmedOrNull(raw.city),
      state: raw.state,
      country: trimmedOrNull(raw.country),
      creditLimit: raw.creditLimit,
      paymentTerms: trimmedOrNull(raw.paymentTerms),
      notes: trimmedOrNull(raw.notes),
      tagIds: raw.tagIds,
      active: raw.active
    };

    this.saving.set(true);
    const editing = this.editingCustomer();
    const request$ = editing
      ? this.customerService.update(editing.id, request)
      : this.customerService.create(request);

    request$.subscribe({
      next: () => {
        this.saving.set(false);
        this.dialogVisible.set(false);
        this.messageService.add({
          severity: 'success',
          summary: this.translate.instant(
            editing ? 'commercialPages.customers.messages.updated' : 'commercialPages.customers.messages.created'
          )
        });
        this.load();
      },
      error: () => this.saving.set(false)
    });
  }

  protected confirmDelete(customer: Customer): void {
    this.confirmationService.confirm({
      header: this.translate.instant('common.confirmDelete.title'),
      message: this.translate.instant('common.confirmDelete.message', { name: customer.name }),
      acceptLabel: this.translate.instant('common.confirmDelete.accept'),
      rejectLabel: this.translate.instant('common.confirmDelete.reject'),
      acceptButtonProps: { severity: 'danger' },
      accept: () => {
        this.customerService.delete(customer.id).subscribe(() => {
          this.messageService.add({
            severity: 'success',
            summary: this.translate.instant('commercialPages.customers.messages.deleted')
          });
          this.load();
        });
      }
    });
  }

  protected location(customer: Customer): string {
    if (customer.city && customer.state) {
      return `${customer.city}/${customer.state}`;
    }
    return customer.city ?? customer.state ?? '-';
  }

  protected chipStyle(color: string | null): Record<string, string> | null {
    return domainChipStyle(color);
  }
}
