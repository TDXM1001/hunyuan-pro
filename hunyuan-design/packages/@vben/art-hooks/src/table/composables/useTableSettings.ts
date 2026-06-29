import { reactive, toRefs } from 'vue'

type TableSize = 'default' | 'large' | 'small'

const tableSettings = reactive({
  isBorder: false,
  isHeaderBackground: false,
  isZebra: false,
  tableSize: 'default' as TableSize,
})

function setTableSize(size: TableSize) {
  tableSettings.tableSize = size
}

function setIsZebra(value: boolean) {
  tableSettings.isZebra = value
}

function setIsBorder(value: boolean) {
  tableSettings.isBorder = value
}

function setIsHeaderBackground(value: boolean) {
  tableSettings.isHeaderBackground = value
}

export function useTableSettings() {
  return {
    ...toRefs(tableSettings),
    setIsBorder,
    setIsHeaderBackground,
    setIsZebra,
    setTableSize,
  }
}

export type { TableSize }
