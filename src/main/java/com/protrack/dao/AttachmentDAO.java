package com.protrack.dao;

import com.protrack.model.TaskAttachment;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object interface for Task Attachment management.
 */
public interface AttachmentDAO {

    TaskAttachment save(TaskAttachment attachment);

    List<TaskAttachment> findByTaskId(int taskId);

    Optional<TaskAttachment> findById(int attachmentId);

    boolean deleteById(int attachmentId);

    int countByTaskId(int taskId);
}
