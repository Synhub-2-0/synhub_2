export type PlanCode = 'FREE' | 'PRO' | 'ULTRA';

/** Un plan del catálogo. `maxLedGroups` es null cuando no tiene límite. */
export interface Plan {
  code: PlanCode;
  name: string;
  monthlyPrice: number;
  maxLedGroups: number | null;
}

/** Mi plan: el actual, cuántos grupos liderados están activos y bloqueados, y el catálogo para compararlos. */
export class Subscription {
  readonly plan: PlanCode;
  readonly planName: string;
  readonly monthlyPrice: number;
  readonly maxLedGroups: number | null;
  readonly activeLedGroups: number;
  readonly lockedLedGroups: number;
  readonly availableSlots: number | null;
  readonly plans: Plan[];

  constructor(props: {
    plan: PlanCode;
    planName: string;
    monthlyPrice: number;
    maxLedGroups: number | null;
    activeLedGroups: number;
    lockedLedGroups: number;
    availableSlots: number | null;
    plans: Plan[];
  }) {
    this.plan = props.plan;
    this.planName = props.planName;
    this.monthlyPrice = props.monthlyPrice;
    this.maxLedGroups = props.maxLedGroups;
    this.activeLedGroups = props.activeLedGroups;
    this.lockedLedGroups = props.lockedLedGroups;
    this.availableSlots = props.availableSlots;
    this.plans = props.plans;
  }

  get isUnlimited(): boolean {
    return this.maxLedGroups === null;
  }

  /** ¿Cabe activar otro grupo bloqueado? */
  get hasFreeSlot(): boolean {
    return this.availableSlots === null || this.availableSlots > 0;
  }
}
