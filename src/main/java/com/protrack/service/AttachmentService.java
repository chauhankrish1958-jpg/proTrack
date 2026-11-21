package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.AttachmentDAO;
import com.protrack.dao.ProjectDAO;
import com.protrack.dao.TaskDAO;
import com.protrack.dao.impl.ActivityDAOImpl;
import com.protrack.dao.impl.AttachmentDAOImpl;
import com.protrack.dao.impl.ProjectDAOImpl;
import com.protrack.dao.impl.TaskDAOImpl;
import com.protrack.exception.AppException;
import com.protrack.exception.UnauthorizedAccessException;
import com.protrack.model.Role;
import com.protrack.model.Task;
import com.protrack.model.TaskAttachment;
import com.protrack.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service managing task file attachments, security validation, and role authorization.
 */
public class AttachmentService {

    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L; // 10 MB

    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".pdf", ".png", ".jpg", ".jpeg", ".gif", ".svg",
            ".txt", ".csv", ".sql", ".json", ".xml",
            ".doc", ".docx", ".xls", ".xlsx",
            ".zip", ".tar", ".gz"
    ));

    private static final Set<String> DANGEROUS_EXTENSIONS = new HashSet<>(Arrays.asList(
            ".jsp", ".jspx", ".sh", ".bat", ".cmd", ".exe", ".war", ".class", ".jar", ".py", ".rb", ".php"
    ));

    private final AttachmentDAO attachmentDAO;
    private final TaskDAO taskDAO;
    private final ProjectDAO projectDAO;
    private final ActivityDAO activityDAO;

    public AttachmentService() {
        this.attachmentDAO = new AttachmentDAOImpl();
        this.taskDAO = new TaskDAOImpl();
        this.projectDAO = new ProjectDAOImpl();
        this.activityDAO = new ActivityDAOImpl();
    }

    public AttachmentService(AttachmentDAO attachmentDAO, TaskDAO taskDAO, ProjectDAO projectDAO, ActivityDAO activityDAO) {
        this.attachmentDAO = attachmentDAO;
        this.taskDAO = taskDAO;
        this.projectDAO = projectDAO;
        this.activityDAO = activityDAO;
    }

    /**
     * Sanitizes raw file name against path traversal and dangerous characters.
     */
    public static String sanitizeFileName(String rawFileName) {
        if (rawFileName == null || rawFileName.trim().isEmpty()) {
            return "unnamed_file";
        }
        // Normalize backslashes to forward slashes for cross-platform path safety
        String normalized = rawFileName.replace('\\', '/');
        // Extract base filename if path was provided
        String clean = Paths.get(normalized).getFileName().toString();
        // Remove null bytes and control chars
        clean = clean.replaceAll("[\\r\\n\\x00]", "");
        // Remove leading dots or slashes
        clean = clean.replaceAll("^[./\\\\]+", "");
        return clean.trim().isEmpty() ? "unnamed_file" : clean.trim();
    }

    /**
     * Validates whether file extension is allowed.
     */
    public static boolean isAllowedExtension(String fileName) {
        if (fileName == null) return false;
        String lower = fileName.toLowerCase();
        for (String dangerous : DANGEROUS_EXTENSIONS) {
            if (lower.endsWith(dangerous)) {
                return false;
            }
        }
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifies user has permission to access or view attachments for a given task.
     */
    public void verifyTaskAccess(Task task, User user) throws AppException {
        if (user == null) {
            throw new UnauthorizedAccessException("Authentication required.");
        }
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        if (user.getRole() == Role.PROJECT_MANAGER) {
            boolean ownsProject = projectDAO.findById(task.getProjectId())
                    .map(p -> p.getManagerId() == user.getId())
                    .orElse(false);
            if (!ownsProject) {
                throw new UnauthorizedAccessException("You can only manage attachments on your own projects.");
            }
            return;
        }
        // TEAM_MEMBER
        boolean isMember = projectDAO.isMemberAssigned(task.getProjectId(), user.getId());
        boolean isAssignee = task.getAssignedTo() != null && task.getAssignedTo().equals(user.getId());
        if (!isMember && !isAssignee) {
            throw new UnauthorizedAccessException("You do not have permission to access attachments for this task.");
        }
    }

    /**
     * Retrieves all attachments for a task after verifying user authorization.
     */
    public List<TaskAttachment> getAttachmentsByTask(int taskId, User user) throws AppException {
        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new AppException("Task not found with ID: " + taskId);
        }
        verifyTaskAccess(taskOpt.get(), user);
        return attachmentDAO.findByTaskId(taskId);
    }

    /**
     * Retrieves a single attachment by ID after verifying access.
     */
    public TaskAttachment getAttachmentById(int attachmentId, User user) throws AppException {
        Optional<TaskAttachment> attOpt = attachmentDAO.findById(attachmentId);
        if (attOpt.isEmpty()) {
            throw new AppException("Attachment not found with ID: " + attachmentId);
        }
        TaskAttachment att = attOpt.get();
        Optional<Task> taskOpt = taskDAO.findById(att.getTaskId());
        if (taskOpt.isEmpty()) {
            throw new AppException("Task not found for attachment: " + attachmentId);
        }
        verifyTaskAccess(taskOpt.get(), user);
        return att;
    }

    /**
     * Uploads and saves a new task attachment.
     */
    public TaskAttachment uploadAttachment(int taskId, User user, String rawFileName, String contentType,
                                           long fileSize, InputStream inputStream, String storageDir) throws AppException {
        if (user == null) {
            throw new UnauthorizedAccessException("Authentication required.");
        }
        if (fileSize <= 0) {
            throw new AppException("Cannot upload an empty file.");
        }
        if (fileSize > MAX_FILE_SIZE) {
            throw new AppException("File size exceeds the 10 MB limit.");
        }

        String sanitizedName = sanitizeFileName(rawFileName);
        if (!isAllowedExtension(sanitizedName)) {
            throw new AppException("File type not allowed. Supported formats: PDF, PNG, JPG, DOCX, XLSX, TXT, CSV, SQL, ZIP.");
        }

        Optional<Task> taskOpt = taskDAO.findById(taskId);
        if (taskOpt.isEmpty()) {
            throw new AppException("Task not found with ID: " + taskId);
        }
        Task task = taskOpt.get();
        verifyTaskAccess(task, user);

        // Ensure storage directory exists
        File dir = new File(storageDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // Unique storage file name to prevent collision and overwrite
        String storageFileName = "task-" + taskId + "-" + System.currentTimeMillis() + "-" +
                                UUID.randomUUID().toString().substring(0, 8) + "-" + sanitizedName;
        File targetFile = new File(dir, storageFileName);

        try (OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new AppException("Failed to store file on disk: " + e.getMessage());
        }

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTaskId(taskId);
        attachment.setUserId(user.getId());
        attachment.setFileName(sanitizedName);
        attachment.setFilePath(targetFile.getPath());
        attachment.setFileSize(fileSize);
        attachment.setContentType(contentType != null ? contentType : "application/octet-stream");

        TaskAttachment saved = attachmentDAO.save(attachment);
        saved.setUploaderName(user.getFullName());

        activityDAO.log(user.getId(), "TASK_ATTACHMENT_UPLOAD",
                user.getFullName() + " attached \"" + sanitizedName + "\" (" + saved.getFormattedFileSize() + ") to task #" + taskId + ".",
                "TASK", taskId);

        return saved;
    }

    /**
     * Deletes an attachment without requiring explicit storageDir.
     */
    public boolean deleteAttachment(int attachmentId, User user) throws AppException {
        return deleteAttachment(attachmentId, user, null);
    }

    /**
     * Deletes an attachment after verifying deletion permissions.
     */
    public boolean deleteAttachment(int attachmentId, User user, String storageDir) throws AppException {
        if (user == null) {
            throw new UnauthorizedAccessException("Authentication required.");
        }
        Optional<TaskAttachment> attOpt = attachmentDAO.findById(attachmentId);
        if (attOpt.isEmpty()) {
            throw new AppException("Attachment not found with ID: " + attachmentId);
        }
        TaskAttachment att = attOpt.get();

        Optional<Task> taskOpt = taskDAO.findById(att.getTaskId());
        if (taskOpt.isEmpty()) {
            throw new AppException("Task not found for attachment: " + attachmentId);
        }
        Task task = taskOpt.get();

        // Deletion permissions: Admin, Project Manager of project, or the user who uploaded it
        boolean canDelete = false;
        if (user.getRole() == Role.ADMIN) {
            canDelete = true;
        } else if (user.getRole() == Role.PROJECT_MANAGER) {
            canDelete = projectDAO.findById(task.getProjectId())
                    .map(p -> p.getManagerId() == user.getId())
                    .orElse(false);
        } else if (att.getUserId() == user.getId()) {
            canDelete = true;
        }

        if (!canDelete) {
            throw new UnauthorizedAccessException("You do not have permission to delete this attachment.");
        }

        boolean deleted = attachmentDAO.deleteById(attachmentId);
        if (!deleted) {
            throw new AppException("Failed to delete attachment from database.");
        }

        // Clean up file on disk if exists
        try {
            if (att.getFilePath() != null) {
                Path path = Paths.get(att.getFilePath());
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
            // Ignored; DB deletion is already committed
        }

        activityDAO.log(user.getId(), "TASK_ATTACHMENT_DELETE",
                user.getFullName() + " removed attachment \"" + att.getFileName() + "\" from task #" + task.getId() + ".",
                "TASK", task.getId());
        return true;
    }
}
