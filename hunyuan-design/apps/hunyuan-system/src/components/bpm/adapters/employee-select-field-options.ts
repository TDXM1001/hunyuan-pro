export interface BpmEmployeeSelectFieldOption {
  field: string;
  label: string;
}

type SchemaNode = Record<string, any>;

function safeParseSchema(schemaJson?: string): unknown {
  if (!schemaJson?.trim()) {
    return [];
  }

  try {
    return JSON.parse(schemaJson);
  } catch {
    return [];
  }
}

function collectSchemaNodes(value: unknown): SchemaNode[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => collectSchemaNodes(item));
  }
  if (!value || typeof value !== 'object') {
    return [];
  }

  const node = value as SchemaNode;
  const children = [
    ...collectSchemaNodes(node.children),
    ...collectSchemaNodes(node.fields),
  ];

  return [node, ...children];
}

function hasEmployeeSelectType(node: SchemaNode) {
  const values = [
    node.type,
    node.component,
    node.props?.type,
    node.props?.component,
  ]
    .filter(Boolean)
    .map((value) => String(value).toLowerCase());

  return values.some(
    (value) => value === 'employee' || value === 'employeeselect',
  );
}

function hasEmployeeFieldName(field: string) {
  const normalized = field.toLowerCase();
  return normalized.includes('employeeid') || normalized.includes('approver');
}

export function extractEmployeeSelectFieldOptions(
  schemaJson?: string,
): BpmEmployeeSelectFieldOption[] {
  const parsed = safeParseSchema(schemaJson);
  const seen = new Set<string>();

  return collectSchemaNodes(parsed)
    .map((node) => {
      const field = typeof node.field === 'string' ? node.field.trim() : '';
      if (!field || seen.has(field)) {
        return undefined;
      }
      if (!hasEmployeeSelectType(node) && !hasEmployeeFieldName(field)) {
        return undefined;
      }

      seen.add(field);
      return {
        field,
        label:
          typeof node.title === 'string' && node.title.trim()
            ? node.title.trim()
            : field,
      };
    })
    .filter(Boolean) as BpmEmployeeSelectFieldOption[];
}
