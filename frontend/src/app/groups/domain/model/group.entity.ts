import {BaseEntity} from '../../../shared/domain/model/base-entity';

export class Group implements BaseEntity {
  private _id: number;
  private _name: string;
  private _imgUrl: string;
  private _description: string;
  private _code: string;
  private _memberCount: number;
  private _locked: boolean;

  constructor(props: {
    id: number;
    name: string;
    imgUrl: string;
    description: string;
    code: string;
    memberCount: number;
    locked?: boolean;
  }) {
    this._id = props.id;
    this._name = props.name;
    this._imgUrl = props.imgUrl;
    this._description = props.description;
    this._code = props.code;
    this._memberCount = props.memberCount;
    this._locked = props.locked ?? false;
  }

  get id(): number {return this._id;}
  get name(): string {return this._name}
  get imgUrl(): string {return this._imgUrl;}
  get description(): string {return this._description;}
  get code(): string {return this._code;}
  get memberCount(): number {return this._memberCount;}
  /** Bloqueado: su líder superó el límite de su plan; es de solo lectura hasta que se active. */
  get locked(): boolean {return this._locked;}

  set id(value: number) {this._id = value;}
  set name(value: string) {this._name = value;}
  set imgUrl(value: string) {this._imgUrl = value;}
  set description(value: string) {this._description = value;}
  set code(value: string) {this._code = value;}
  set memberCount(value: number) {this._memberCount = value;}
  set locked(value: boolean) {this._locked = value;}
}
