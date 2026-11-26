import { MemberProductivity } from './member-productivity';

describe('MemberProductivity', () => {
  const metrics = {
    overview: {
      type: 'TASK_OVERVIEW_MEMBER',
      total: 6,
      details: { DONE: 2, EXPIRED: 1, IN_PROGRESS: 1, ON_HOLD: 1, COMPLETED: 1 },
    },
    rescheduled: { type: 'RESCHEDULED_TASKS_MEMBER', rescheduled: 3, total: 6, rescheduledMemberIds: [7] },
    avgCompletion: { type: 'AVG_COMPLETION_TIME_MEMBER', days: 2.25, completedTasks: 2 },
  };

  it('agrupa en curso y en espera, y separa terminadas y vencidas', () => {
    const component = new MemberProductivity();
    component.metrics = metrics;

    expect(component.done).toBe(2);
    expect(component.expired).toBe(1);
    expect(component.inProgress).toBe(3);
  });

  it('emite el id del integrante elegido como número', () => {
    const component = new MemberProductivity();
    const emitted: number[] = [];
    component.memberSelected.subscribe((id: number) => emitted.push(id));

    component.select(7);

    expect(emitted).toEqual([7]);
  });
});
