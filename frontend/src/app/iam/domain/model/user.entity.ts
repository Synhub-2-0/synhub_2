import {BaseEntity} from '../../../shared/domain/model/base-entity';
import {AccountRoles} from './account-roles.entity';

export class User implements BaseEntity {
  private _id: number;
  private _username: string;
  private _accountRoles: AccountRoles;

  constructor(
    props:{
      id: number,
      username: string,
      accountRoles: AccountRoles,
    }) {
    this._id = props.id;
    this._username = props.username;
    this._accountRoles = props.accountRoles;
  }

  get id(): number { return this._id; }
  get username(): string { return this._username; }
  get accountRoles(): string[] { return this._accountRoles.roles}

  set id(value: number) { this._id = value; }
  set username(value: string) { this._username = value; }
  set accountRoles(value: string[]) { this._accountRoles.roles = value; }
}
