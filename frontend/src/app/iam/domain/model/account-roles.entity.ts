export class AccountRoles {
  private _roles: string[];

  constructor(props: {roles: string[]}) {
    this._roles = props.roles;
  }

  get roles(): string[] {return this._roles;}
  set roles(roles: string[]) {this._roles = roles;}
}
