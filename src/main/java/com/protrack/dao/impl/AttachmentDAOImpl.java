package com.protrack.dao.impl;

import com.protrack.dao.AttachmentDAO;
import com.protrack.exception.DatabaseOperationException;
import com.protrack.model.TaskAttachment;
import com.protrack.util.DBConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JDBC implementation of AttachmentDAO.
 */
public class AttachmentDAOImpl implements AttachmentDAO {

    private TaskAttachment mapRow(ResultSet rs) throws SQLException {
        TaskAttachment att = new TaskAttachment();
        att.setId(rs.getInt("attachment_id"));
        att.setTaskId(rs.getInt("task_id"));
        att.setUserId(rs.getInt("user_id"));
        att.setFileName(rs.getString("file_name"));
        att.setFilePath(rs.getString("file_path"));
        att.setFileSize(rs.getLong("file_size"));
        att.setContentType(rs.getString("content_type"));
        att.setUploadedAt(rs.getTimestamp("uploaded_at"));
        try {
            att.setUploaderName(rs.getString("uploader_name"));
        } catch (SQLException ignored) {
            // column might not be present if not joined
        }
        return att;
    }

    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        String sql = "INSERT INTO task_attachments (task_id, user_id, file_name, file_path, file_size, content_type) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, attachment.getTaskId());
            ps.setInt(2, attachment.getUserId());
            ps.setString(3, attachment.getFileName());
            ps.setString(4, attachment.getFilePath());
            ps.setLong(5, attachment.getFileSize());
            ps.setString(6, attachment.getContentType());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    attachment.setId(rs.getInt(1));
                }
            }
            return attachment;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to save task attachment: " + attachment.getFileName(), e);
        }
    }

    @Override
    public List<TaskAttachment> findByTaskId(int taskId) {
        String sql = "SELECT a.*, u.full_name AS uploader_name " +
                     "FROM task_attachments a " +
                     "LEFT JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.task_id = ? " +
                     "ORDER BY a.uploaded_at DESC";
        List<TaskAttachment> list = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to fetch attachments for task ID: " + taskId, e);
        }
    }

    @Override
    public Optional<TaskAttachment> findById(int attachmentId) {
        String sql = "SELECT a.*, u.full_name AS uploader_name " +
                     "FROM task_attachments a " +
                     "LEFT JOIN users u ON a.user_id = u.user_id " +
                     "WHERE a.attachment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attachmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to find attachment by ID: " + attachmentId, e);
        }
    }

    @Override
    public boolean deleteById(int attachmentId) {
        String sql = "DELETE FROM task_attachments WHERE attachment_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, attachmentId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to delete attachment ID: " + attachmentId, e);
        }
    }

    @Override
    public int countByTaskId(int taskId) {
        String sql = "SELECT COUNT(*) FROM task_attachments WHERE task_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseOperationException("Failed to count attachments for task ID: " + taskId, e);
        }
    }
}
