import {Group} from '../../../groups/domain/model/group.entity';
import {Profile} from '../../../iam/domain/model/profile.entity';
import {BaseEntity} from '../../../shared/domain/model/base-entity';

export class Invitation implements BaseEntity {
  _id: number;
  _group: Group;
  _profile: Profile;

  constructor(props: {
    id: number;
    group: Group;
    profile: Profile;
  }) {
    this._id = props.id;
    this._group = props.group;
    this._profile = props.profile;
  }

  get id(): number {return this._id;}
  get group(): Group {return this._group;}
  get profile(): Profile {return this._profile;}

  set id(value: number) {this._id = value;}
  set group(value: Group) {this._group = value;}
  set profile(value: Profile) {this._profile = value;}
}
