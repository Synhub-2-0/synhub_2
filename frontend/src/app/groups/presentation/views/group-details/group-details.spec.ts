import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { GroupDetails } from './group-details';

describe('GroupDetails', () => {
  let component: GroupDetails;
  let fixture: ComponentFixture<GroupDetails>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GroupDetails],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    fixture = TestBed.createComponent(GroupDetails);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
