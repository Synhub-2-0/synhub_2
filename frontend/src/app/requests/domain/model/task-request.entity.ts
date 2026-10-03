import {BaseEntity} from '../../../shared/domain/model/base-entity';
import {Task} from '../../../tasks/domain/model/task.entity';

export type RequestType = 'SUBMISSION' | 'MODIFICATION' | 'EXPIRED';
export type RequestStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export class TaskRequest implements BaseEntity {
  private _id: number;
  private _description: string;
  private _requestType: RequestType;
  private _requestStatus: RequestStatus;
  private _task: Task;

  constructor(props: {
    id: number;
    description: string;
    requestType: RequestType;
    requestStatus: RequestStatus;
    task: Task;
  }) {
    this._id = props.id;
    this._description = props.description;
    this._requestType = props.requestType;
    this._requestStatus = props.requestStatus;
    this._task = props.task;
  }

  get id(): number {return this._id;}
  get description(): string {return this._description;}
  get requestType(): RequestType {return this._requestType;}
  get requestStatus(): RequestStatus {return this._requestStatus;}
  get task(): Task {return this._task;}

  set id(value: number) {this._id = value;}
  set description(value: string) {this._description = value;}
  set requestType(value: RequestType) {this._requestType = value;}
  set requestStatus(value: RequestStatus) {this._requestStatus = value;}
  set task(value: Task) {this._task = value;}
}
