import {User} from '../../../iam/domain/model/user.entity';

export class GroupUser {
  private _user: User;
  private _roleInGroup: string;

  constructor(groupUser: { user: User; roleInGroup: string }) {
    this._user = groupUser.user;
    this._roleInGroup = groupUser.roleInGroup;
  }

  get user(): User {return this._user;}
  get roleInGroup() {return this._roleInGroup;}

  set user(user: User) {this._user = user;}
  set roleInGroup(roleInGroup: string) {this._roleInGroup = roleInGroup;}
}
