export class CreateGroupCommand {

  private _name: string;
  private _description: string;
  private _imgUrl: string;

  constructor(
    props: {
      name: string,
      description: string,
      imgUrl: string
    }) {
    this._name = props.name;
    this._description = props.description;
    this._imgUrl = props.imgUrl;
  }

  get name(): string { return this._name }
  get description(): string { return this._description }
  get imgUrl(): string { return this._imgUrl }

  set name(name: string) { this._name = name; }
  set description(description: string) { this._description = description; }
  set imgUrl(imgUrl: string) { this._imgUrl = imgUrl; }
}
