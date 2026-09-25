import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LeaderGroups } from './leader-groups';

describe('LeaderGroups', () => {
  let component: LeaderGroups;
  let fixture: ComponentFixture<LeaderGroups>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LeaderGroups],
    }).compileComponents();

    fixture = TestBed.createComponent(LeaderGroups);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
