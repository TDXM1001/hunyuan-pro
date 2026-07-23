package com.hunyuan.sa.admin.module.organization.position.application;

import com.hunyuan.sa.admin.module.organization.OrganizationBusinessException;
import com.hunyuan.sa.admin.module.organization.OrganizationErrorCode;
import com.hunyuan.sa.admin.module.organization.OrganizationModuleAvailability;
import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionCommand;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionEmployeeReferencePort;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionRepository;
import com.hunyuan.sa.base.common.domain.ResponseDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 岗位目录应用服务。
 */
@Service
public class OrganizationPositionApplicationService implements OrganizationPositionFacade {

    @Resource
    private PositionRepository positionRepository;

    @Resource
    private PositionEmployeeReferencePort employeeReferencePort;

    @Resource
    private OrganizationModuleAvailability moduleAvailability;

    @Override
    @Transactional(readOnly = true)
    public List<Position> list() {
        moduleAvailability.requireEnabled();
        return positionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Position get(Long positionId) {
        moduleAvailability.requireEnabled();
        return positionRepository.findById(positionId).orElseThrow(this::positionNotFound);
    }

    @Override
    @Transactional
    public ResponseDTO<Long> create(PositionCommand command) {
        moduleAvailability.requireEnabled();
        Position position = normalize(command, null);
        validateName(position.positionName(), null);
        return ResponseDTO.ok(positionRepository.insert(position));
    }

    @Override
    @Transactional
    public ResponseDTO<String> update(Long positionId, PositionCommand command) {
        moduleAvailability.requireEnabled();
        if (!positionRepository.exists(positionId)) {
            throw positionNotFound();
        }
        Position position = normalize(command, positionId);
        validateName(position.positionName(), positionId);
        positionRepository.update(position);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional
    public ResponseDTO<String> delete(Long positionId) {
        moduleAvailability.requireEnabled();
        if (!positionRepository.exists(positionId)) {
            throw positionNotFound();
        }
        validateEmployeeReferences(positionId);
        positionRepository.delete(positionId);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional
    public ResponseDTO<String> deleteBatch(List<Long> positionIds) {
        moduleAvailability.requireEnabled();
        List<Long> normalizedIds = normalizeIds(positionIds);
        for (Long positionId : normalizedIds) {
            if (!positionRepository.exists(positionId)) {
                throw positionNotFound();
            }
        }
        for (Long positionId : normalizedIds) {
            validateEmployeeReferences(positionId);
        }
        normalizedIds.forEach(positionRepository::delete);
        return ResponseDTO.ok();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Position> findForCollaboration(Long positionId) {
        return positionRepository.findById(positionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Position> listForCollaboration() {
        return positionRepository.findAll();
    }

    private void validateEmployeeReferences(Long positionId) {
        int referenceCount = employeeReferencePort.countNonDeletedEmployees(positionId);
        if (referenceCount > 0) {
            throw new OrganizationBusinessException(
                    OrganizationErrorCode.POSITION_IN_USE,
                    "岗位仍被 " + referenceCount + " 名员工引用，请先完成岗位改派");
        }
    }

    private Position normalize(PositionCommand command, Long positionId) {
        if (command == null || command.positionName() == null) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_POSITION, "岗位名称不能为空");
        }
        String positionName = command.positionName().trim();
        if (positionName.isEmpty() || positionName.length() > 200) {
            throw new OrganizationBusinessException(
                    OrganizationErrorCode.INVALID_POSITION,
                    "岗位名称长度必须为 1-200 个字符");
        }
        if (command.sort() == null || command.sort() < 0) {
            throw new OrganizationBusinessException(OrganizationErrorCode.INVALID_POSITION, "排序值不能小于 0");
        }
        return new Position(
                positionId,
                positionName,
                trimToNull(command.positionLevel()),
                command.sort(),
                trimToNull(command.remark()),
                null,
                null);
    }

    private void validateName(String positionName, Long excludedPositionId) {
        if (positionRepository.existsByName(positionName, excludedPositionId)) {
            throw new OrganizationBusinessException(OrganizationErrorCode.POSITION_NAME_DUPLICATED);
        }
    }

    private OrganizationBusinessException positionNotFound() {
        return new OrganizationBusinessException(OrganizationErrorCode.POSITION_NOT_FOUND);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()).stream().toList();
    }
}
