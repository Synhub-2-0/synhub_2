import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { BaseApi } from '../../shared/infrastructure/base-api';
import { TaskClassification } from '../domain/model/task-classification.entity';

const aiEndpointUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderAiEndpointPath}`;

@Injectable({ providedIn: 'root' })
export class AiApi extends BaseApi {
  constructor(private readonly http: HttpClient) {
    super();
  }

  /** POST /api/v1/ai/classify */
  classifyTask(payload: { title: string; description: string; dueDate?: Date }): Observable<TaskClassification> {
    return this.http.post<TaskClassification>(`${aiEndpointUrl}/classify`, {
      ...payload,
      dueDate: payload.dueDate?.toISOString(),
    });
  }

  /** GET /api/v1/ai/groups/{groupId}/report (solo líder) */
  getGroupReport(groupId: number): Observable<string> {
    return this.http
      .get<{ report: string }>(`${aiEndpointUrl}/groups/${groupId}/report`)
      .pipe(map((response) => response.report));
  }
}
