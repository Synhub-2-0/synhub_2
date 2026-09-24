export class SignUpCommand {
  get username(): string {return this._username;}
  get password(): string {return this._password;}
  get name(): string {return this._name;}
  get surname(): string {return this._surname;}
  get email(): string {return this._email;}
  get imgUrl(): string {return this._imgUrl;}
  get accountRoles(): string[] { return this._accountRoles; }

  set username(value: string) {this._username = value;}
  set password(value: string) {this._password = value;}
  set name(value: string) {this._name = value;}
  set surname(value: string) {this._surname = value;}
  set email(value: string) {this._email = value;}
  set imgUrl(value: string) {this._imgUrl = value;}
  set accountRoles(value: string[]) {this._accountRoles = value;}

  private _username: string;
  private _password: string;
  private _name: string;
  private _surname: string;
  private _email: string;
  private _imgUrl: string;
  private _accountRoles: string[];

  constructor(
    props: {
      username: string,
      password: string,
      name: string,
      surname: string,
      email: string,
      imgUrl: string,
      accountRoles: string[],
    }) {
    this._username = props.username;
    this._password = props.password;
    this._name = props.name;
    this._surname = props.surname;
    this._email = props.email;
    this._imgUrl = props.imgUrl;
    this._accountRoles = props.accountRoles;
  }
}
