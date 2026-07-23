export interface PositionRecord {
  createTime?: null | string;
  positionId: number;
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
  updateTime?: null | string;
}

export interface PositionCommand {
  positionLevel?: null | string;
  positionName: string;
  remark?: null | string;
  sort: number;
}

export interface OrganizationPositionClient {
  create(command: PositionCommand): Promise<number>;
  delete(positionId: number): Promise<void>;
  get(positionId: number): Promise<PositionRecord>;
  list(): Promise<PositionRecord[]>;
  update(positionId: number, command: PositionCommand): Promise<void>;
}
