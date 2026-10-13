import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { SignInPage } from './sign-in-page';

describe('SignInPage', () => {
  let component: SignInPage;
  let fixture: ComponentFixture<SignInPage>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SignInPage],
    }).compileComponents();

    fixture = TestBed.createComponent(SignInPage);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain('Tu sesión expiró');
  });

  it('AUDIT SEC-10: con ?expired=1 avisa que la sesión expiró', async () => {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [SignInPage],
      providers: [{ provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ expired: '1' }) } } }],
    }).compileComponents();
    const expired = TestBed.createComponent(SignInPage);
    expired.detectChanges();
    expect(expired.nativeElement.textContent).toContain('Tu sesión expiró');
  });
});
