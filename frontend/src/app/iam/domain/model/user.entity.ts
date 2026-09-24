import {BaseEntity} from '../../../shared/domain/model/base-entity';
import {AccountRoles} from './account-roles.entity';

export class User implements BaseEntity {
  private _id: number;
  private _username: string;
  private _name: string;
  private _surname: string;
  private _email: string;
  private _imgUrl: string;
  private _accountRoles: AccountRoles;

  constructor(
    props:{
      id: number,
      username: string,
      name: string,
      surname: string,
      email: string,
      imgUrl: string,
      accountRoles: AccountRoles,
    }) {
    this._id = props.id;
    this._username = props.username;
    this._name = props.name;
    this._surname = props.surname;
    this._email = props.email;
    this._imgUrl = props.imgUrl;
    this._accountRoles = props.accountRoles;
  }

  get id(): number { return this._id; }
  get username(): string { return this._username; }
  get name(): string { return this._name; }
  get surname(): string { return this._surname; }
  get email(): string { return this._email; }
  get imgUrl(): string { return this._imgUrl; }
  get accountRoles(): string[] { return this._accountRoles.roles}

  set id(value: number) { this._id = value; }
  set username(value: string) { this._username = value; }
  set name(value: string) { this._name = value; }
  set surname(value: string) { this._surname = value; }
  set email(value: string) { this._email = value; }
  set imgUrl(value: string) { this._imgUrl = value; }
  set accountRoles(value: string[]) { this._accountRoles.roles = value; }


}
