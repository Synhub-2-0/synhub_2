import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, map, Observable, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Subscription } from '../domain/model/subscription.entity';
import { ChangePlanCommand } from '../domain/model/change-plan.command';
import { SubscriptionResource } from './subscriptions.response';
import { SubscriptionsAssembler } from './subscriptions.assembler';

const subscriptionUrl = `${environment.platformProviderApiBaseUrl}${environment.platformProviderSubscriptionsEndpointPath}/me`;

export class SubscriptionsApiEndpoint {
  private readonly assembler = new SubscriptionsAssembler();

  constructor(private readonly http: HttpClient) {}

  /** GET /api/v1/subscriptions/me */
  getMySubscription(): Observable<Subscription> {
    return this.http.get<SubscriptionResource>(subscriptionUrl).pipe(
      map((resource) => this.assembler.toEntityFromResource(resource)),
      catchError(() => throwError(() => new Error('No se pudo cargar tu plan.'))),
    );
  }

  /** PUT /api/v1/subscriptions/me (pago simulado: el cuerpo no lleva datos de pago) */
  changePlan(command: ChangePlanCommand): Observable<Subscription> {
    return this.http.put<SubscriptionResource>(subscriptionUrl, this.assembler.toRequestFromCommand(command)).pipe(
      map((resource) => this.assembler.toEntityFromResource(resource)),
      catchError((error: HttpErrorResponse) => throwError(() => new Error(this.changeErrorMessage(error)))),
    );
  }

  // The backend answers in English: the status is mapped to a user-facing text.
  private changeErrorMessage(error: HttpErrorResponse): string {
    switch (error.status) {
      case 400: return 'El plan elegido no es válido.';
      case 409: return 'Elige exactamente los grupos que seguirán activos con el nuevo plan.';
      default: return 'No se pudo cambiar el plan.';
    }
  }
}
