package com.hunyuan.sa.admin.module.organization.position.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hunyuan.sa.admin.module.organization.position.domain.Position;
import com.hunyuan.sa.admin.module.organization.position.domain.PositionRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 岗位目录仓储适配器。
 */
@Repository
public class PositionRepositoryAdapter implements PositionRepository {

    @Resource
    private PositionPersistenceMapper mapper;

    @Override
    public List<Position> findAll() {
        return mapper.selectList(new LambdaQueryWrapper<PositionPersistenceEntity>()
                        .eq(PositionPersistenceEntity::getDeletedFlag, false)
                        .orderByAsc(PositionPersistenceEntity::getSort)
                        .orderByAsc(PositionPersistenceEntity::getPositionId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<Position> findById(Long positionId) {
        if (positionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.selectOne(new LambdaQueryWrapper<PositionPersistenceEntity>()
                        .eq(PositionPersistenceEntity::getPositionId, positionId)
                        .eq(PositionPersistenceEntity::getDeletedFlag, false)
                        .last("LIMIT 1")))
                .map(this::toDomain);
    }

    @Override
    public boolean exists(Long positionId) {
        return findById(positionId).isPresent();
    }

    @Override
    public boolean existsByName(String positionName, Long excludedPositionId) {
        LambdaQueryWrapper<PositionPersistenceEntity> wrapper = new LambdaQueryWrapper<PositionPersistenceEntity>()
                .eq(PositionPersistenceEntity::getPositionName, positionName)
                .eq(PositionPersistenceEntity::getDeletedFlag, false)
                .ne(excludedPositionId != null, PositionPersistenceEntity::getPositionId, excludedPositionId)
                .last("LIMIT 1");
        return mapper.selectCount(wrapper) > 0;
    }

    @Override
    public Long insert(Position position) {
        PositionPersistenceEntity entity = toEntity(position);
        entity.setDeletedFlag(false);
        mapper.insert(entity);
        return entity.getPositionId();
    }

    @Override
    public void update(Position position) {
        mapper.updateById(toEntity(position));
    }

    @Override
    public void delete(Long positionId) {
        mapper.deleteById(positionId);
    }

    private Position toDomain(PositionPersistenceEntity entity) {
        return new Position(
                entity.getPositionId(),
                entity.getPositionName(),
                entity.getPositionLevel(),
                entity.getSort(),
                entity.getRemark(),
                entity.getCreateTime(),
                entity.getUpdateTime());
    }

    private PositionPersistenceEntity toEntity(Position position) {
        PositionPersistenceEntity entity = new PositionPersistenceEntity();
        entity.setPositionId(position.positionId());
        entity.setPositionName(position.positionName());
        entity.setPositionLevel(position.positionLevel());
        entity.setSort(position.sort());
        entity.setRemark(position.remark());
        return entity;
    }
}
