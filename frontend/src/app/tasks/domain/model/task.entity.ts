import {BaseEntity} from '../../../shared/domain/model/base-entity';

export class Task implements BaseEntity {
  private _id: number;
  private _title: string;
  private _description: string;
  private _dueDate: Date;
  private _createdAt: Date;
  private _updatedAt: Date;
  private _status: string;

  constructor(props: {
    id: number;
    title: string;
    description: string;
    dueDate: Date;
    createdAt: Date;
    updatedAt: Date;
    status: string;
  }) {
    this._id = props.id;
    this._title = props.title;
    this._description = props.description;
    this._dueDate = props.dueDate;
    this._createdAt = props.createdAt;
    this._updatedAt = props.updatedAt;
    this._status = props.status;
  }

  get id(): number {return this._id;}
  get title(): string {return this._title;}
  get description(): string {return this._description;}
  get dueDate(): Date {return this._dueDate;}
  get createdAt(): Date {return this._createdAt;}
  get updatedAt(): Date {return this._updatedAt;}
  get status(): string {return this._status;}

  set id(value: number) {this._id = value;}
  set title(value: string) {this._title = value;}
  set description(value: string) {this._description = value;}
  set dueDate(value: Date) {this._dueDate = value;}
  set createdAt(value: Date) {this._createdAt = value;}
  set updatedAt(value: Date) {this._updatedAt = value;}
  set status(value: string) {this._status = value;}
}
