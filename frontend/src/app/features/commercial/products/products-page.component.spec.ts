import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { provideTranslateService } from '@ngx-translate/core';
import { ConfirmationService, MessageService } from 'primeng/api';
import { of } from 'rxjs';
import { Product } from '../../../core/models/product.model';
import { DomainValueService } from '../../../core/services/domain-value.service';
import { ProductService } from '../../../core/services/product.service';
import { ProductsPageComponent } from './products-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

function productFixture(overrides: Partial<Product> = {}): Product {
  return {
    id: 'product-1',
    code: 'PRD-001000',
    name: 'Consultoria de implantacao',
    description: null,
    sku: null,
    category: null,
    unit: null,
    unitPrice: 1500,
    costPrice: null,
    service: true,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    ...overrides
  };
}

describe('ProductsPageComponent', () => {
  let fixture: ComponentFixture<ProductsPageComponent>;
  let component: ProductsPageComponent;
  let productServiceStub: jasmine.SpyObj<ProductService>;

  beforeEach(async () => {
    localStorage.clear();

    productServiceStub = jasmine.createSpyObj<ProductService>('ProductService', [
      'list',
      'create',
      'update',
      'delete'
    ]);
    productServiceStub.list.and.returnValue(of(emptyPage));

    await TestBed.configureTestingModule({
      imports: [ProductsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideRouter([]),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        MessageService,
        ConfirmationService,
        { provide: ProductService, useValue: productServiceStub },
        { provide: DomainValueService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('shows the empty state when there are no products', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('[data-testid="generic-table-empty"]')).toBeTruthy();
  });

  it('requires a name before saving', () => {
    component['openCreateDialog']();
    component['save']();

    expect(component['form'].controls.name.invalid).toBeTrue();
    expect(productServiceStub.create).not.toHaveBeenCalled();
  });

  it('creates a product as active and not a service by default', () => {
    component['openCreateDialog']();

    expect(component['form'].controls.active.value).toBeTrue();
    expect(component['form'].controls.service.value).toBeFalse();
  });

  it('loads the linked values of a product when editing', () => {
    component['openEditDialog'](productFixture({ name: 'Licenca anual', unitPrice: 999 }));

    expect(component['form'].controls.name.value).toBe('Licenca anual');
    expect(component['form'].controls.unitPrice.value).toBe(999);
    expect(component['form'].controls.service.value).toBeTrue();
  });

  it('sends the category filter to the API only when it is selected', () => {
    component['categoryFilter'].set('cat-1');
    component['onFilterChange']();

    expect(productServiceStub.list.calls.mostRecent().args[0].categoryId).toBe('cat-1');

    component['categoryFilter'].set(null);
    component['onFilterChange']();

    expect(productServiceStub.list.calls.mostRecent().args[0].categoryId).toBeUndefined();
  });
});
