import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AuthenticationSection } from './authentication-section';

describe('AuthenticationSection', () => {
  let component: AuthenticationSection;
  let fixture: ComponentFixture<AuthenticationSection>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AuthenticationSection],
    }).compileComponents();

    fixture = TestBed.createComponent(AuthenticationSection);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
