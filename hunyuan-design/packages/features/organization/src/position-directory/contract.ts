/** 岗位目录的服务端记录和页面可编辑字段。 */
export interface PositionRecord {
  /** 岗位列表记录，positionId 是更新和删除时使用的稳定主键。 */
  createTime?: null | string;
  positionId: number;
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
  updateTime?: null | string;
}

export interface PositionCommand {
  /** 新增/修改岗位时提交的字段，主键不放在这里。 */
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
}

export interface OrganizationPositionClient {
  /** 岗位目录页面使用的增删改查动作。 */
  create(command: PositionCommand): Promise<number>;
  delete(positionId: number): Promise<void>;
  get(positionId: number): Promise<PositionRecord>;
  list(): Promise<PositionRecord[]>;
  update(positionId: number, command: PositionCommand): Promise<void>;
}
