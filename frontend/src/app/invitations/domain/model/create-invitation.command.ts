export class CreateInvitationCommand {

  private _groupId: number;
  private _userId: number;

  constructor(
    props: {
      groupId: number,
      userId: number
    }) {
    this._groupId = props.groupId;
    this._userId = props.userId;
  }

  get groupId(): number { return this._groupId }
  get userId(): number { return this._userId }

  set groupId(groupId: number) { this._groupId = groupId; }
  set userId(userId: number) { this._userId = userId; }
}
