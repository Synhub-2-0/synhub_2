import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { GroupDetails } from './group-details';
import { GroupsStore } from '../../../application/groups.store';

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

  it('HUM-05: quitar a un integrante pide confirmación', () => {
    const store = TestBed.inject(GroupsStore);
    const remove = vi.spyOn(store, 'removeMember').mockImplementation(() => undefined);
    component.groupId.set(10);

    component.askRemoval(2);
    expect(component.pendingRemoval()).toBe(2);
    component.cancelRemoval();
    expect(remove).not.toHaveBeenCalled();

    component.askRemoval(2);
    component.confirmRemoval();
    expect(remove).toHaveBeenCalledWith(10, 2, expect.any(Function));
    expect(component.pendingRemoval()).toBeNull();
  });

  it('HUM-05: salir del grupo', () => {
    const store = TestBed.inject(GroupsStore);
    const leave = vi.spyOn(store, 'leaveGroup').mockImplementation(() => undefined);
    component.groupId.set(10);
    component.askRemoval('self');
    component.confirmRemoval();
    expect(leave).toHaveBeenCalledWith(10, expect.anything());
  });
});
