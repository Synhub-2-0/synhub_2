export interface SignUpRequest {
  username: string;
  name: string;
  surname: string;
  imgUrl: string;
  email: string;
  password: string;
  accountRoles: string[];
}
