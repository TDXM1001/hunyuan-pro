package com.hunyuan.sa.admin.module.organization.position.domain;

import java.util.List;
import java.util.Optional;

/**
 * 岗位目录持久化边界。
 */
public interface PositionRepository {

    List<Position> findAll();

    Optional<Position> findById(Long positionId);

    boolean exists(Long positionId);

    boolean existsByName(String positionName, Long excludedPositionId);

    Long insert(Position position);

    void update(Position position);

    void delete(Long positionId);
}
