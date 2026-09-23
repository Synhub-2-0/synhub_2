import {BaseEntity} from '../../../shared/domain/model/base-entity';

export class User implements BaseEntity {
  private _id: number;

  private _username: string;

  constructor(
    props:{
      id: number,
      username: string
    }) {
    this._id = props.id;
    this._username = props.username;
  }

  get id(): number { return this._id; }
  get username(): string { return this._username; }

  set id(value: number) { this._id = value; }
  set username(value: string) { this._username = value; }
}
