import { PlanCode } from '../domain/model/subscription.entity';

export interface PlanResource {
  plan: PlanCode;
  name: string;
  monthlyPrice: number;
  maxLedGroups: number | null;
}

export interface SubscriptionResource {
  plan: PlanCode;
  planName: string;
  monthlyPrice: number;
  maxLedGroups: number | null;
  activeLedGroups: number;
  lockedLedGroups: number;
  availableSlots: number | null;
  plans: PlanResource[];
}

export interface ChangePlanRequest {
  plan: PlanCode;
  activeGroupIds?: number[];
}
