import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { PipelineStage, PipelineStageRequest } from '../models/pipeline.model';
import { PageResponse } from '../models/page.model';
import { ReorderItem } from '../models/reorder.model';

@Injectable({ providedIn: 'root' })
export class PipelineStageService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = environment.apiBaseUrl;

  private stagesUrl(pipelineId: string): string {
    return `${this.baseUrl}/pipelines/${pipelineId}/stages`;
  }

  list(pipelineId: string): Observable<PageResponse<PipelineStage>> {
    return this.http.get<PageResponse<PipelineStage>>(this.stagesUrl(pipelineId), {
      params: { size: 100, sort: 'displayOrder' }
    });
  }

  create(pipelineId: string, request: PipelineStageRequest): Observable<PipelineStage> {
    return this.http.post<PipelineStage>(this.stagesUrl(pipelineId), request);
  }

  update(pipelineId: string, stageId: string, request: PipelineStageRequest): Observable<PipelineStage> {
    return this.http.put<PipelineStage>(`${this.stagesUrl(pipelineId)}/${stageId}`, request);
  }

  delete(pipelineId: string, stageId: string): Observable<void> {
    return this.http.delete<void>(`${this.stagesUrl(pipelineId)}/${stageId}`);
  }

  reorder(pipelineId: string, items: ReorderItem[]): Observable<PipelineStage[]> {
    return this.http.put<PipelineStage[]>(`${this.stagesUrl(pipelineId)}/reorder`, { items });
  }
}
