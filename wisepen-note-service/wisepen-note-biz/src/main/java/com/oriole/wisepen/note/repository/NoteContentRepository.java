package com.oriole.wisepen.note.repository;

import com.oriole.wisepen.note.domain.entity.NoteContentEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoteContentRepository extends MongoRepository<NoteContentEntity, String> {
    void deleteByResourceIdIn(List<String> resourceIds);
}
