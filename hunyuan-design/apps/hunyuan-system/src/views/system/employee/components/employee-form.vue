<script setup lang="ts">
import type {
  DepartmentRecord,
  EmployeeAddForm,
  EmployeeRecord,
  EmployeeUpdateForm,
  PositionRecord,
  RoleRecord,
} from '#/api/system/organization';

import { computed, reactive, ref, watch } from 'vue';

import {
  ElButton,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElMessage,
  ElOption,
  ElRadioButton,
  ElRadioGroup,
  ElSelect,
  ElSpace,
  type FormInstance,
  type FormRules,
} from 'element-plus';

import { addEmployee, updateEmployee } from '#/api/system/organization';

defineOptions({ name: 'EmployeeForm' });

export interface EmployeeFormProps {
  visible: boolean;
  mode: 'add' | 'edit';
  departments: DepartmentRecord[];
  positions: PositionRecord[];
  roles: RoleRecord[];
  employee?: EmployeeRecord;
}

const props = withDefaults(defineProps<EmployeeFormProps>(), {
  employee: undefined,
});

const emit = defineEmits<{
  'update:visible': [value: boolean];
  success: [password?: string];
}>();

const formRef = ref<FormInstance>();
const loading = ref(false);
const generatedPassword = ref<string>();

const formData = reactive<EmployeeAddForm | EmployeeUpdateForm>({
  actualName: '',
  loginName: '',
  departmentId: null,
  disabledFlag: false,
  positionId: null,
  roleIdList: [],
  phone: '',
  email: '',
  gender: undefined,
});

const rules: FormRules = {
  actualName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  departmentId: [
    { required: true, message: '请选择所属部门', trigger: 'change' },
  ],
  loginName: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    {
      pattern: /^[a-z\d_-]{4,20}$/i,
      message: '账号为4-20位字母、数字、下划线或短横线',
      trigger: 'blur',
    },
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    {
      pattern: /^1[3-9]\d{9}$/,
      message: '请输入正确的手机号',
      trigger: 'blur',
    },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱', trigger: 'blur' },
  ],
};

const title = computed(() => (props.mode === 'add' ? '新增员工' : '编辑员工'));

const departmentOptions = computed(() =>
  [...props.departments].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
);

const positionOptions = computed(() =>
  [...props.positions].sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0)),
);

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      generatedPassword.value = undefined;
      if (props.mode === 'edit' && props.employee) {
        Object.assign(formData, {
          employeeId: props.employee.employeeId,
          actualName: props.employee.actualName,
          loginName: props.employee.loginName,
          departmentId: props.employee.departmentId,
          disabledFlag: props.employee.disabledFlag ?? false,
          positionId: props.employee.positionId,
          roleIdList: props.employee.roleIdList || [],
          phone: props.employee.phone || '',
          email: props.employee.email || '',
          gender: props.employee.gender,
        });
      } else {
        Object.assign(formData, {
          actualName: '',
          loginName: '',
          departmentId: null,
          disabledFlag: false,
          positionId: null,
          roleIdList: [],
          phone: '',
          email: '',
          gender: undefined,
        });
      }
      formRef.value?.clearValidate();
    }
  },
);

function handleClose() {
  emit('update:visible', false);
}

async function handleConfirm() {
  if (!formRef.value) return;

  await formRef.value.validate();

  loading.value = true;
  try {
    if (props.mode === 'add') {
      const password = await addEmployee(formData as EmployeeAddForm);
      generatedPassword.value = password;
      ElMessage.success('新增成功');
      emit('success', password);
    } else {
      await updateEmployee(formData as EmployeeUpdateForm);
      ElMessage.success('更新成功');
      emit('success');
      handleClose();
    }
  } finally {
    loading.value = false;
  }
}

function handleCopyPassword() {
  if (generatedPassword.value) {
    navigator.clipboard.writeText(generatedPassword.value);
    ElMessage.success('密码已复制到剪贴板');
  }
}

function handlePasswordDialogClose() {
  generatedPassword.value = undefined;
  handleClose();
}
</script>

<template>
  <ElDialog
    :model-value="visible"
    :title="title"
    width="600px"
    @close="handleClose"
  >
    <ElForm
      v-if="!generatedPassword"
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="90px"
    >
      <ElFormItem label="员工姓名" prop="actualName">
        <ElInput
          v-model="formData.actualName"
          clearable
          placeholder="请输入员工姓名"
        />
      </ElFormItem>

      <ElFormItem label="登录账号" prop="loginName">
        <ElInput
          v-model="formData.loginName"
          :disabled="mode === 'edit'"
          clearable
          placeholder="请输入登录账号"
        />
      </ElFormItem>

      <ElFormItem label="所属部门" prop="departmentId">
        <ElSelect
          v-model="formData.departmentId"
          clearable
          filterable
          placeholder="请选择部门"
          style="width: 100%"
        >
          <ElOption
            v-for="dept in departmentOptions"
            :key="dept.departmentId"
            :label="dept.departmentName"
            :value="dept.departmentId"
          />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="岗位" prop="positionId">
        <ElSelect
          v-model="formData.positionId"
          clearable
          filterable
          placeholder="请选择岗位"
          style="width: 100%"
        >
          <ElOption
            v-for="pos in positionOptions"
            :key="pos.positionId"
            :label="pos.positionName"
            :value="pos.positionId"
          />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="角色" prop="roleIdList">
        <ElSelect
          v-model="formData.roleIdList"
          clearable
          collapse-tags
          collapse-tags-tooltip
          filterable
          multiple
          placeholder="请选择角色"
          style="width: 100%"
        >
          <ElOption
            v-for="role in roles"
            :key="role.roleId"
            :label="role.roleName"
            :value="role.roleId"
          />
        </ElSelect>
      </ElFormItem>

      <ElFormItem label="手机号" prop="phone">
        <ElInput
          v-model="formData.phone"
          clearable
          placeholder="请输入手机号"
        />
      </ElFormItem>

      <ElFormItem label="邮箱" prop="email">
        <ElInput
          v-model="formData.email"
          clearable
          placeholder="请输入邮箱"
        />
      </ElFormItem>

      <ElFormItem label="性别" prop="gender">
        <ElRadioGroup v-model="formData.gender">
          <ElRadioButton :value="1">男</ElRadioButton>
          <ElRadioButton :value="2">女</ElRadioButton>
        </ElRadioGroup>
      </ElFormItem>

      <ElFormItem label="状态" prop="disabledFlag">
        <ElRadioGroup v-model="formData.disabledFlag">
          <ElRadioButton :value="false">启用</ElRadioButton>
          <ElRadioButton :value="true">停用</ElRadioButton>
        </ElRadioGroup>
      </ElFormItem>
    </ElForm>

    <div v-else class="password-result">
      <div class="password-result__icon">✓</div>
      <div class="password-result__title">员工添加成功</div>
      <div class="password-result__content">
        <div class="password-result__label">初始密码</div>
        <div class="password-result__value">{{ generatedPassword }}</div>
        <div class="password-result__tip">请妥善保管并告知员工修改密码</div>
      </div>
    </div>

    <template #footer>
      <ElSpace v-if="!generatedPassword">
        <ElButton @click="handleClose">取消</ElButton>
        <ElButton :loading="loading" type="primary" @click="handleConfirm">
          确定
        </ElButton>
      </ElSpace>
      <ElSpace v-else>
        <ElButton @click="handleCopyPassword">复制密码</ElButton>
        <ElButton type="primary" @click="handlePasswordDialogClose">
          关闭
        </ElButton>
      </ElSpace>
    </template>
  </ElDialog>
</template>

<style scoped>
.password-result {
  padding: 24px 0;
  text-align: center;
}

.password-result__icon {
  background: var(--el-color-success-light-9);
  border-radius: 50%;
  color: var(--el-color-success);
  font-size: 48px;
  font-weight: bold;
  height: 80px;
  line-height: 80px;
  margin: 0 auto 16px;
  width: 80px;
}

.password-result__title {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
}

.password-result__content {
  background: var(--el-fill-color-light);
  border-radius: 8px;
  margin: 0 auto;
  max-width: 400px;
  padding: 20px;
}

.password-result__label {
  color: var(--el-text-color-secondary);
  font-size: 14px;
  margin-bottom: 8px;
}

.password-result__value {
  background: var(--el-color-primary-light-9);
  border-radius: 4px;
  color: var(--el-color-primary);
  font-family: 'Courier New', monospace;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
  padding: 12px;
}

.password-result__tip {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  margin-top: 12px;
}
</style>
