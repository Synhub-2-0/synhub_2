import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoGroupLeader } from './no-group-leader';

describe('NoGroupLeader', () => {
  let component: NoGroupLeader;
  let fixture: ComponentFixture<NoGroupLeader>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NoGroupLeader],
    }).compileComponents();

    fixture = TestBed.createComponent(NoGroupLeader);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
