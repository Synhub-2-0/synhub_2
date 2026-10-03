import { BaseEntity } from '../../../shared/domain/model/base-entity';

export type TaskStatus = 'ON_HOLD' | 'IN_PROGRESS' | 'COMPLETED' | 'DONE' | 'EXPIRED';

export interface TaskUser {
  id: number;
  username: string;
  name: string;
  surname: string;
  imgUrl: string;
  email: string;
  accountRoles: string[];
}

export interface TaskGroup {
  id: number;
  name: string;
  imgUrl: string;
  description: string;
  code: string;
  memberCount: number;
  leader: TaskUser;
}

export class Task implements BaseEntity {
  private _id: number;
  private _title: string;
  private _description: string;
  private _dueDate: Date;
  private _createdAt: Date;
  private _updatedAt: Date;
  private _status: TaskStatus;
  private _group?: TaskGroup;
  private _assignedTo?: TaskUser;

  constructor(props: {
    id: number;
    title: string;
    description: string;
    dueDate: Date;
    createdAt: Date;
    updatedAt: Date;
    status: TaskStatus | string;
    group?: TaskGroup;
    assignedTo?: TaskUser;
  }) {
    this._id = props.id;
    this._title = props.title;
    this._description = props.description;
    this._dueDate = props.dueDate;
    this._createdAt = props.createdAt;
    this._updatedAt = props.updatedAt;
    this._status = props.status as TaskStatus;
    this._group = props.group;
    this._assignedTo = props.assignedTo;
  }

  get id(): number {
    return this._id;
  }
  get title(): string {
    return this._title;
  }
  get description(): string {
    return this._description;
  }
  get dueDate(): Date {
    return this._dueDate;
  }
  get createdAt(): Date {
    return this._createdAt;
  }
  get updatedAt(): Date {
    return this._updatedAt;
  }
  get status(): TaskStatus {
    return this._status;
  }
  get group(): TaskGroup | undefined {
    return this._group;
  }
  get assignedTo(): TaskUser | undefined {
    return this._assignedTo;
  }

  set id(value: number) {
    this._id = value;
  }
  set title(value: string) {
    this._title = value;
  }
  set description(value: string) {
    this._description = value;
  }
  set dueDate(value: Date) {
    this._dueDate = value;
  }
  set createdAt(value: Date) {
    this._createdAt = value;
  }
  set updatedAt(value: Date) {
    this._updatedAt = value;
  }
  set status(value: TaskStatus) {
    this._status = value;
  }
  set group(value: TaskGroup | undefined) {
    this._group = value;
  }
  set assignedTo(value: TaskUser | undefined) {
    this._assignedTo = value;
  }
}
