import { Subscription } from '../domain/model/subscription.entity';
import { ChangePlanCommand } from '../domain/model/change-plan.command';
import { ChangePlanRequest, SubscriptionResource } from './subscriptions.response';

export class SubscriptionsAssembler {
  toEntityFromResource(resource: SubscriptionResource): Subscription {
    return new Subscription({
      plan: resource.plan,
      planName: resource.planName,
      monthlyPrice: resource.monthlyPrice,
      maxLedGroups: resource.maxLedGroups ?? null,
      activeLedGroups: resource.activeLedGroups,
      lockedLedGroups: resource.lockedLedGroups,
      availableSlots: resource.availableSlots ?? null,
      plans: (resource.plans ?? []).map((plan) => ({
        code: plan.plan,
        name: plan.name,
        monthlyPrice: plan.monthlyPrice,
        maxLedGroups: plan.maxLedGroups ?? null,
      })),
    });
  }

  toRequestFromCommand(command: Pick<ChangePlanCommand, 'plan' | 'activeGroupIds'>): ChangePlanRequest {
    return command.activeGroupIds
      ? { plan: command.plan, activeGroupIds: command.activeGroupIds }
      : { plan: command.plan };
  }
}
