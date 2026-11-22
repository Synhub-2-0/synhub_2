import { PlanCode } from './subscription.entity';

/** Cambiar de plan (pago simulado: no lleva datos de pago). Al bajar de límite, los grupos que siguen activos. */
export class ChangePlanCommand {
  constructor(
    public readonly plan: PlanCode,
    public readonly activeGroupIds?: number[],
  ) {}
}
