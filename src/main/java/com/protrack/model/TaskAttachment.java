package com.protrack.model;

import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Domain model representing a file attachment associated with a Task.
 */
public class TaskAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private int taskId;
    private int userId;
    private String uploaderName;
    private String fileName;
    private String filePath;
    private long fileSize;
    private String contentType;
    private Timestamp uploadedAt;

    public TaskAttachment() {
    }

    public TaskAttachment(int id, int taskId, int userId, String fileName, String filePath, long fileSize, String contentType, Timestamp uploadedAt) {
        this.id = id;
        this.taskId = taskId;
        this.userId = userId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
        this.uploadedAt = uploadedAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTaskId() {
        return taskId;
    }

    public void setTaskId(int taskId) {
        this.taskId = taskId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Timestamp getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Timestamp uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    /**
     * Returns human-readable formatted file size (e.g. "450 B", "24.5 KB", "3.2 MB").
     */
    public String getFormattedFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024.0));
        }
    }

    /**
     * Returns a Bootstrap icon class matching the file's extension or MIME type.
     */
    public String getFileIconClass() {
        if (fileName == null) {
            return "bi-file-earmark-text-fill text-secondary";
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "bi-file-earmark-pdf-fill text-danger";
        } else if (lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".svg")) {
            return "bi-file-earmark-image-fill text-primary";
        } else if (lower.endsWith(".sql") || lower.endsWith(".java") || lower.endsWith(".js") || lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".html") || lower.endsWith(".css")) {
            return "bi-file-earmark-code-fill text-info";
        } else if (lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz") || lower.endsWith(".rar")) {
            return "bi-file-earmark-zip-fill text-warning";
        } else if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            return "bi-file-earmark-word-fill text-primary";
        } else if (lower.endsWith(".xls") || lower.endsWith(".xlsx") || lower.endsWith(".csv")) {
            return "bi-file-earmark-excel-fill text-success";
        } else {
            return "bi-file-earmark-text-fill text-secondary";
        }
    }
}
