import {BaseEntity} from '../../../shared/domain/model/base-entity';
import {GroupUser} from './group-user.entity';

export class Group implements BaseEntity {
  private _id: number;
  private _name: string;
  private _imgUrl: string;
  private _description: string;
  private _code: string;
  private _usersInGroup: GroupUser[];

  constructor(props: {
    id: number;
    name: string;
    imgUrl: string;
    description: string;
    code: string;
    usersInGroup: GroupUser[];
  }) {
    this._id = props.id;
    this._name = props.name;
    this._imgUrl = props.imgUrl;
    this._description = props.description;
    this._code = props.code;
    this._usersInGroup = props.usersInGroup;
  }

  get id(): number {return this._id;}
  get name(): string {return this._name}
  get imgUrl(): string {return this._imgUrl;}
  get description(): string {return this._description;}
  get code(): string {return this._code;}
  get usersInGroup(): GroupUser[] {return this._usersInGroup;}

  set id(value: number) {this._id = value;}
  set name(value: string) {this._name = value;}
  set imgUrl(value: string) {this._imgUrl = value;}
  set description(value: string) {this._description = value;}
  set code(value: string) {this._code = value;}
  set usersInGroup(value: GroupUser[]) {this._usersInGroup = value;}
}
