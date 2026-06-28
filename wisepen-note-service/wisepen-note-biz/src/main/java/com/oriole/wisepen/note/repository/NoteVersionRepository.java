package com.oriole.wisepen.note.repository;

import com.oriole.wisepen.note.domain.entity.NoteVersionEntity;
import com.oriole.wisepen.note.api.domain.enums.VersionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoteVersionRepository extends MongoRepository<NoteVersionEntity, String> {

    /** 分页查询指定资源的所有版本记录，并按版本号降序排列 */
    Page<NoteVersionEntity> findByResourceIdOrderByVersionDesc(String resourceId, Pageable pageable);

    /** 查找指定资源最近的某类型版本 */
    Optional<NoteVersionEntity> findFirstByResourceIdAndTypeOrderByVersionDesc(
            String resourceId, VersionType type);

    /** 查找指定资源最近的版本 */
    Optional<NoteVersionEntity> findFirstByResourceIdOrderByVersionDesc(String resourceId);

    /** 查找指定资源的特定版本 */
    Optional<NoteVersionEntity> findByResourceIdAndVersion(String resourceId, Integer version);

    /** 查询指定资源在指定版本号（含）之前的最新特定类型版本记录 */
    Optional<NoteVersionEntity> findFirstByResourceIdAndTypeAndVersionLessThanEqualOrderByVersionDesc(
            String resourceId, VersionType type, Integer version);

    /** 查询指定资源在指定版本号之后的所有特定类型版本记录，并按版本号升序排列 */
    List<NoteVersionEntity> findByResourceIdAndVersionGreaterThanAndTypeOrderByVersionAsc(
            String resourceId, Integer version, VersionType type);

    /** 查询指定资源在指定版本号区间内的所有特定类型版本记录，并按版本号升序排列, 查询区间为左开右闭(startVersion, endVersion] **/
    List<NoteVersionEntity> findByResourceIdAndVersionGreaterThanAndVersionLessThanEqualAndTypeOrderByVersionAsc(
            String resourceId, Integer startVersion, Integer endVersion, VersionType type);

    void deleteByResourceIdIn(List<String> resourceIds);
}
