import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideTranslateService } from '@ngx-translate/core';
import { of } from 'rxjs';
import { Report } from '../../core/models/report.model';
import { AdminUserService } from '../../core/services/admin-user.service';
import { ReportService } from '../../core/services/report.service';
import { ReportsPageComponent } from './reports-page.component';

const emptyPage = { content: [], page: 0, size: 10, totalElements: 0, totalPages: 0, last: true };

const opportunitiesReport: Report = {
  report: 'OPPORTUNITIES',
  groupBy: 'STAGE',
  measured: true,
  totalCount: 3,
  totalAmount: 4500,
  generatedAt: '2026-01-01T00:00:00Z',
  rows: [
    { label: 'Proposta', count: 2, total: 3000, percentage: 66.67 },
    { label: null, count: 1, total: 1500, percentage: 33.33 }
  ]
};

describe('ReportsPageComponent', () => {
  let fixture: ComponentFixture<ReportsPageComponent>;
  let component: ReportsPageComponent;
  let reportServiceStub: jasmine.SpyObj<ReportService>;

  beforeEach(async () => {
    localStorage.clear();

    reportServiceStub = jasmine.createSpyObj<ReportService>('ReportService', ['load', 'export']);
    reportServiceStub.load.and.returnValue(of(opportunitiesReport));
    reportServiceStub.export.and.returnValue(of(new Blob(['csv'])));

    await TestBed.configureTestingModule({
      imports: [ReportsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        provideTranslateService({ lang: 'pt-BR', fallbackLang: 'pt-BR' }),
        { provide: ReportService, useValue: reportServiceStub },
        { provide: AdminUserService, useValue: { list: () => of(emptyPage) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ReportsPageComponent);
    component = fixture.componentInstance;
    fixture.componentRef.setInput('report', 'opportunities');
    fixture.detectChanges();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('selects the first grouping of the report and loads it', () => {
    expect(component['groupBy']()).toBe('STAGE');
    expect(reportServiceStub.load).toHaveBeenCalledWith('opportunities', jasmine.objectContaining({ groupBy: 'STAGE' }));
  });

  it('offers only the groupings of the current report', () => {
    const values = component['groupByOptions']().map((option) => option.value);

    expect(values).toContain('LOSS_REASON');
    expect(values).not.toContain('CLIENT_TYPE');
  });

  it('keeps the selected grouping when a filter changes', () => {
    component['groupBy'].set('OUTCOME');
    component['userId'].set('user-1');
    component['onFilterChange']();

    expect(component['groupBy']()).toBe('OUTCOME');
    expect(reportServiceStub.load).toHaveBeenCalledWith(
      'opportunities',
      jasmine.objectContaining({ groupBy: 'OUTCOME', userId: 'user-1' })
    );
  });

  it('clears the filters and reloads', () => {
    component['userId'].set('user-1');
    component['clearFilters']();

    expect(component['userId']()).toBeNull();
    expect(reportServiceStub.load.calls.mostRecent().args[1].userId).toBeNull();
  });
});
