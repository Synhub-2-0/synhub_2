import {RequestType} from './task-request.entity';

export class CreateRequestCommand {

  private _taskId: number;
  private _description: string;
  private _requestType: RequestType;

  constructor(
    props: {
      taskId: number,
      description: string,
      requestType: RequestType
    }) {
    this._taskId = props.taskId;
    this._description = props.description;
    this._requestType = props.requestType;
  }

  get taskId(): number { return this._taskId }
  get description(): string { return this._description }
  get requestType(): RequestType { return this._requestType }

  set taskId(taskId: number) { this._taskId = taskId; }
  set description(description: string) { this._description = description; }
  set requestType(requestType: RequestType) { this._requestType = requestType; }
}
