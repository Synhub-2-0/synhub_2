import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoGroupMember } from './no-group-member';

describe('NoGroupMember', () => {
  let component: NoGroupMember;
  let fixture: ComponentFixture<NoGroupMember>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoGroupMember],
    }).compileComponents();

    fixture = TestBed.createComponent(NoGroupMember);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
