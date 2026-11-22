import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SubscriptionsApiEndpoint } from './subscriptions.api-endpoint';
import { Subscription } from '../domain/model/subscription.entity';
import { ChangePlanCommand } from '../domain/model/change-plan.command';

@Injectable({ providedIn: 'root' })
export class SubscriptionsApi {
  private readonly endpoint = new SubscriptionsApiEndpoint(inject(HttpClient));

  getMySubscription(): Observable<Subscription> {
    return this.endpoint.getMySubscription();
  }

  changePlan(command: ChangePlanCommand): Observable<Subscription> {
    return this.endpoint.changePlan(command);
  }
}
