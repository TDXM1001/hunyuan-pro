package com.hunyuan.sa.admin.module.system.department.service;

import jakarta.annotation.Resource;
import com.hunyuan.sa.admin.module.organization.department.application.OrganizationDepartmentFacade;
import com.hunyuan.sa.admin.module.organization.department.domain.Department;
import com.hunyuan.sa.admin.module.organization.department.domain.DepartmentCommand;
import com.hunyuan.sa.admin.module.system.department.domain.entity.DepartmentEntity;
import com.hunyuan.sa.admin.module.system.department.domain.form.DepartmentAddForm;
import com.hunyuan.sa.admin.module.system.department.domain.form.DepartmentUpdateForm;
import com.hunyuan.sa.admin.module.system.department.domain.vo.DepartmentTreeVO;
import com.hunyuan.sa.admin.module.system.department.domain.vo.DepartmentVO;
import com.hunyuan.sa.admin.module.system.department.manager.DepartmentCacheManager;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门 service
 *
 * @Author 1024创新实验室-主任: 卓大
 * @Date 2022-01-12 20:37:48
 * @Wechat zhuoda1024
 * @Email lab1024@163.com
 * @Copyright <a href="https://1024lab.net">1024创新实验室</a>
 */
@Service
public class DepartmentService {

    @Resource
    private OrganizationDepartmentFacade organizationDepartmentFacade;

    @Resource
    private DepartmentCacheManager departmentCacheManager;

    // ---------------------------- 增加、修改、删除 ----------------------------

    /**
     * 新增添加部门
     */

    public ResponseDTO<String> addDepartment(DepartmentAddForm departmentAddForm) {
        organizationDepartmentFacade.createForCompatibility(toCommand(departmentAddForm));
        return ResponseDTO.ok();
    }


    /**
     * 更新部门信息
     */
    public ResponseDTO<String> updateDepartment(DepartmentUpdateForm updateDTO) {
        return organizationDepartmentFacade.updateForCompatibility(updateDTO.getDepartmentId(), toCommand(updateDTO));
    }

    /**
     * 根据id删除部门
     * 1、需要判断当前部门是否有子部门,有子部门则不允许删除
     * 2、需要判断当前部门是否有员工，有员工则不能删除
     */
    public ResponseDTO<String> deleteDepartment(Long departmentId) {
        return organizationDepartmentFacade.deleteForCompatibility(departmentId);
    }

    /**
     * 清除自身以及下级的id列表缓存
     */
    private void clearCache() {
        departmentCacheManager.clearCache();
    }

    // ---------------------------- 查询 ----------------------------

    /**
     * 获取部门树形结构
     */
    public ResponseDTO<List<DepartmentTreeVO>> departmentTree() {
        List<DepartmentTreeVO> treeVOList = departmentCacheManager.buildTree(listAll());
        return ResponseDTO.ok(treeVOList);
    }


    /**
     * 自身以及所有下级的部门id列表
     */
    public List<Long> selfAndChildrenIdList(Long departmentId) {
        return departmentCacheManager.selfAndChildrenIdList(departmentId, listAll());
    }


    /**
     * 获取所有部门
     */
    public List<DepartmentVO> listAll() {
        return organizationDepartmentFacade.listForCompatibility().stream().map(this::toLegacyView).toList();
    }


    /**
     * 获取部门
     */
    public DepartmentVO getDepartmentById(Long departmentId) {
        return organizationDepartmentFacade.findForCompatibility(departmentId)
                .map(this::toLegacyView)
                .orElse(null);
    }

    /**
     * 获取部门路径：/公司/研发部/产品组
     */
    public String getDepartmentPath(Long departmentId) {
        Map<Long, DepartmentVO> departmentMap = new HashMap<>();
        for (DepartmentVO department : listAll()) {
            departmentMap.put(department.getDepartmentId(), department);
        }
        List<String> names = new ArrayList<>();
        Long cursor = departmentId;
        while (cursor != null && cursor != 0L) {
            DepartmentVO department = departmentMap.get(cursor);
            if (department == null) {
                break;
            }
            names.add(0, department.getDepartmentName());
            cursor = department.getParentId();
        }
        return String.join("/", names);
    }

    private DepartmentCommand toCommand(DepartmentAddForm form) {
        return new DepartmentCommand(form.getDepartmentName(), form.getManagerId(), form.getParentId(), form.getSort());
    }

    private DepartmentVO toLegacyView(Department department) {
        DepartmentVO view = new DepartmentVO();
        view.setDepartmentId(department.departmentId());
        view.setDepartmentName(department.departmentName());
        view.setManagerId(department.managerId());
        view.setManagerName(department.managerName());
        view.setParentId(department.parentId());
        view.setSort(department.sort());
        view.setCreateTime(department.createTime());
        view.setUpdateTime(department.updateTime());
        return view;
    }

}
