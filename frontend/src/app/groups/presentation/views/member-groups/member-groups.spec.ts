import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { MemberGroups } from './member-groups';
import { GroupsStore } from '../../../application/groups.store';
import { InvitationsStore } from '../../../../invitations/application/invitations.store';

describe('MemberGroups', () => {
  let component: MemberGroups;
  let fixture: ComponentFixture<MemberGroups>;
  let joinGroupByCode: ReturnType<typeof vi.fn>;

  beforeEach(async () => {
    joinGroupByCode = vi.fn();
    await TestBed.configureTestingModule({
      imports: [MemberGroups],
      providers: [
        {
          provide: GroupsStore,
          useValue: { loadGroups: vi.fn(), joinGroupByCode, loading: signal(false), memberGroups: signal([]), joining: signal(false) },
        },
        { provide: InvitationsStore, useValue: { invitations: signal([]), loading: signal(false), loadInvitations: vi.fn() } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MemberGroups);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('HUM-05: el panel permite unirse con un código', () => {
    component.toggleJoinPanel();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#joinCode')).toBeTruthy();

    component.joinCode = 'abc123xyz';
    component.joinByCode();
    expect(joinGroupByCode).toHaveBeenCalledWith('abc123xyz', expect.any(Function));

    joinGroupByCode.mock.calls[0][1]();
    expect(component.joinCode).toBe('');
    expect(component.showJoinPanel()).toBe(false);
  });

  it('un código vacío no hace nada', () => {
    component.joinCode = '   ';
    component.joinByCode();
    expect(joinGroupByCode).not.toHaveBeenCalled();
  });
});
