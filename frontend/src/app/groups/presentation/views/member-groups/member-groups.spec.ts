import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MemberGroups } from './member-groups';

describe('MemberGroups', () => {
  let component: MemberGroups;
  let fixture: ComponentFixture<MemberGroups>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MemberGroups],
    }).compileComponents();

    fixture = TestBed.createComponent(MemberGroups);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
