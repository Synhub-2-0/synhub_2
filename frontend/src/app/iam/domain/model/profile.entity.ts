import {BaseEntity} from '../../../shared/domain/model/base-entity';

export class Profile implements BaseEntity {
  private _id: number;
  private _name: string;
  private _surname: string;
  private _email: string;
  private _imgUrl: string;
  private _userId: number;

  constructor(
    props:{
      id: number,
      name: string,
      surname: string,
      email: string,
      imgUrl: string,
      userId: number,
    }) {
    this._id = props.id;
    this._name = props.name;
    this._surname = props.surname;
    this._email = props.email;
    this._imgUrl = props.imgUrl;
    this._userId = props.userId;
  }

  get id(): number { return this._id; }
  get name(): string { return this._name; }
  get surname(): string { return this._surname; }
  get email(): string { return this._email; }
  get imgUrl(): string { return this._imgUrl; }
  get userId(): number { return this._userId; }

  set id(value: number) { this._id = value; }
  set name(value: string) { this._name = value; }
  set surname(value: string) { this._surname = value; }
  set email(value: string) { this._email = value; }
  set imgUrl(value: string) { this._imgUrl = value; }
  set userId(value: number) { this._userId = value; }
}
