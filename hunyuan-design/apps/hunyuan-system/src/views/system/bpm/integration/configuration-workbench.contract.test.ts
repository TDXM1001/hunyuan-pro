import { describe, expect, it } from 'vitest';
import source from './configuration-workbench.vue?raw';

describe('M6 configuration workbench contract', () => {
  it('rebuilds dynamic columns through the table hook after switching tabs', () => {
    expect(source).toContain('const {columns,columnChecks,resetColumns}=useTableColumns(columnsFactory)');
    expect(source).toContain('resetColumns();await nextTick()');
    expect(source).not.toContain('columns.value=');
    expect(source).toContain("subscriptions:{label:'事件订阅'");
    expect(source).toContain("['connectorKey','连接器']");
    expect(source).toContain("['endpointOperation','操作']");
    expect(source).toContain('grid-template-columns:repeat(5,minmax(0,1fr))');
    expect(source).toContain('transform:none!important');
  });
});
