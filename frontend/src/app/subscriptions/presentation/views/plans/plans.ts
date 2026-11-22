import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../../groups/application/groups.store';
import { SubscriptionsStore } from '../../../application/subscriptions.store';
import { ChangePlanCommand } from '../../../domain/model/change-plan.command';
import { Plan } from '../../../domain/model/subscription.entity';

@Component({
  selector: 'app-plans',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  templateUrl: './plans.html',
  styleUrl: './plans.css',
})
export class Plans implements OnInit {
  readonly subscriptionsStore = inject(SubscriptionsStore);
  readonly groupsStore = inject(GroupsStore);

  /** Plan being confirmed in the simulated checkout. */
  readonly selectedPlan = signal<Plan | null>(null);
  /** Led groups that stay active when the new plan has a lower limit. */
  readonly keptGroupIds = signal<number[]>([]);

  readonly activeLedGroups = computed(() => this.groupsStore.leaderGroups().filter((group) => !group.locked));

  /** Going to a limit lower than the active groups: the leader chooses which ones stay active. */
  readonly needsSelection = computed(() => {
    const limit = this.selectedPlan()?.maxLedGroups ?? null;
    return limit !== null && this.activeLedGroups().length > limit;
  });

  ngOnInit(): void {
    this.subscriptionsStore.load();
    this.groupsStore.loadGroups();
  }

  /** How much of the plan's active-group limit is in use (0 when there is no limit). */
  usagePercent(): number {
    const sub = this.subscriptionsStore.subscription();
    if (!sub || !sub.maxLedGroups) return 0;
    return Math.min(100, (sub.activeLedGroups / sub.maxLedGroups) * 100);
  }

  isCurrent(plan: Plan): boolean {
    return this.subscriptionsStore.subscription()?.plan === plan.code;
  }

  limitLabel(plan: Plan): string {
    if (plan.maxLedGroups === null) return 'Grupos liderados ilimitados';
    return `${plan.maxLedGroups} grupos liderados activos`;
  }

  priceLabel(plan: Plan): string {
    return plan.monthlyPrice === 0 ? '$0' : `$${plan.monthlyPrice} / mes`;
  }

  startCheckout(plan: Plan): void {
    if (this.isCurrent(plan)) return;
    this.selectedPlan.set(plan);
    const limit = plan.maxLedGroups;
    // Suggestion: the oldest groups stay active (the leader can change the choice).
    const ids = this.activeLedGroups().map((group) => group.id).sort((a, b) => a - b);
    this.keptGroupIds.set(limit !== null && ids.length > limit ? ids.slice(0, limit) : []);
  }

  cancelCheckout(): void {
    this.selectedPlan.set(null);
    this.keptGroupIds.set([]);
  }

  toggleGroup(groupId: number): void {
    const limit = this.selectedPlan()?.maxLedGroups ?? null;
    this.keptGroupIds.update((ids) => {
      if (ids.includes(groupId)) return ids.filter((id) => id !== groupId);
      return limit !== null && ids.length >= limit ? ids : [...ids, groupId].sort((a, b) => a - b);
    });
  }

  canConfirm(): boolean {
    if (!this.selectedPlan() || this.subscriptionsStore.changing()) return false;
    return !this.needsSelection() || this.keptGroupIds().length === this.selectedPlan()!.maxLedGroups;
  }

  /** Simulated payment: nothing is charged or stored. */
  confirm(): void {
    const plan = this.selectedPlan();
    if (!plan || !this.canConfirm()) return;
    const kept = this.needsSelection() ? this.keptGroupIds() : undefined;
    this.subscriptionsStore.changePlan(new ChangePlanCommand(plan.code, kept), () => {
      this.cancelCheckout();
      this.groupsStore.loadGroups();
    });
  }
}
