import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { GroupsStore } from '../../../application/groups.store';
import { GroupList } from '../../components/group-list/group-list';
import { NoGroupMember } from '../../components/no-group-member/no-group-member';
import { InvitationsStore } from '../../../../invitations/application/invitations.store';

@Component({
  selector: 'app-member-groups',
  standalone: true,
  imports: [CommonModule, MatIconModule, GroupList, NoGroupMember],
  templateUrl: './member-groups.html',
  styleUrl: './member-groups.css',
})
export class MemberGroups implements OnInit {
  readonly groupsStore = inject(GroupsStore);
  readonly invitationsStore = inject(InvitationsStore);
  readonly showJoinPanel = signal(false);

  get invitations(): ReturnType<InvitationsStore['invitations']> {
    return this.invitationsStore.invitations();
  }

  ngOnInit(): void {
    this.groupsStore.loadGroups();
    this.invitationsStore.loadInvitations();
  }

  toggleJoinPanel(): void {
    this.showJoinPanel.update(value => !value);
  }

  acceptInvitation(invitationId: number): void {
    this.invitationsStore.acceptInvitation(invitationId, () => {
      this.showJoinPanel.set(false);
      this.groupsStore.loadGroups();
    });
  }

  declineInvitation(invitationId: number): void {
    this.invitationsStore.declineInvitation(invitationId);
  }

  /*
   * TODO: Reactivar cuando el backend exponga la búsqueda de grupos por código
   * y el flujo de solicitud para unirse a un grupo.
   */
}
