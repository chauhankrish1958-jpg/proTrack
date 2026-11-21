package com.protrack.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.protrack.exception.AppException;
import com.protrack.model.Role;
import com.protrack.model.TaskAttachment;
import com.protrack.model.User;
import com.protrack.service.AttachmentService;
import com.protrack.util.CSRFTokenUtil;
import com.protrack.util.DateUtil;
import com.protrack.util.ValidationUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Servlet handling task attachment uploads, downloads, JSON listing, and deletions.
 */
@WebServlet(name = "AttachmentServlet", urlPatterns = {"/attachment", "/attachment/download", "/attachment/delete"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB buffer
        maxFileSize = 10 * 1024 * 1024,       // 10 MB per file
        maxRequestSize = 20 * 1024 * 1024     // 20 MB max total request
)
public class AttachmentServlet extends HttpServlet {

    private final AttachmentService attachmentService = new AttachmentService();
    private final Gson gson = new Gson();

    private String getStorageDirectory() {
        String appPath = getServletContext().getRealPath("");
        if (appPath != null) {
            File webUploads = new File(appPath, "uploads/attachments");
            if (!webUploads.exists()) {
                webUploads.mkdirs();
            }
            return webUploads.getAbsolutePath();
        }
        File localUploads = new File("uploads/attachments");
        if (!localUploads.exists()) {
            localUploads.mkdirs();
        }
        return localUploads.getAbsolutePath();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in to access attachments.");
            return;
        }

        String path = request.getServletPath();

        // 1. File Download Endpoint
        if ("/attachment/download".equals(path)) {
            handleDownload(request, response, currentUser);
            return;
        }

        // 2. Task Attachments JSON List Endpoint
        int taskId = ValidationUtil.parsePositiveInt(request.getParameter("taskId"), 0);
        if (taskId <= 0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Invalid task ID\"}");
            return;
        }

        try {
            List<TaskAttachment> attachments = attachmentService.getAttachmentsByTask(taskId, currentUser);

            JsonArray jsonArray = new JsonArray();
            for (TaskAttachment att : attachments) {
                JsonObject obj = new JsonObject();
                obj.addProperty("id", att.getId());
                obj.addProperty("taskId", att.getTaskId());
                obj.addProperty("fileName", att.getFileName());
                obj.addProperty("fileSize", att.getFormattedFileSize());
                obj.addProperty("fileIcon", att.getFileIconClass());
                obj.addProperty("uploaderName", att.getUploaderName() != null ? att.getUploaderName() : "Team Member");
                obj.addProperty("uploadedAt", att.getUploadedAt() != null ? DateUtil.formatDisplayDateTime(att.getUploadedAt()) : "");

                boolean canDelete = (currentUser.getRole() == Role.ADMIN)
                        || (currentUser.getId() == att.getUserId())
                        || (currentUser.getRole() == Role.PROJECT_MANAGER);
                obj.addProperty("canDelete", canDelete);
                jsonArray.add(obj);
            }

            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(gson.toJson(jsonArray));
        } catch (AppException e) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (Exception e) {
            getServletContext().log("Error fetching attachments for task " + taskId, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Internal server error\"}");
        }
    }

    private void handleDownload(HttpServletRequest request, HttpServletResponse response, User currentUser)
            throws IOException {
        int attachmentId = ValidationUtil.parsePositiveInt(request.getParameter("id"), 0);
        if (attachmentId <= 0) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid attachment ID.");
            return;
        }

        try {
            TaskAttachment att = attachmentService.getAttachmentById(attachmentId, currentUser);
            File file = new File(att.getFilePath());

            // Fallback search in storage directory if absolute path was moved
            if (!file.exists()) {
                File storageFallback = new File(getStorageDirectory(), file.getName());
                if (storageFallback.exists()) {
                    file = storageFallback;
                } else {
                    File localFallback = new File("uploads/attachments", file.getName());
                    if (localFallback.exists()) {
                        file = localFallback;
                    }
                }
            }

            if (!file.exists()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Attachment file is missing from server storage.");
                return;
            }

            String mimeType = att.getContentType() != null ? att.getContentType() : "application/octet-stream";
            response.setContentType(mimeType);
            response.setContentLengthLong(file.length());

            String encodedFileName = URLEncoder.encode(att.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + att.getFileName() + "\"; filename*=UTF-8''" + encodedFileName);
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

            try (FileInputStream in = new FileInputStream(file);
                 OutputStream out = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        } catch (AppException e) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            getServletContext().log("Error downloading attachment " + attachmentId, e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to download attachment.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);
        User currentUser = (session != null) ? (User) session.getAttribute("currentUser") : null;
        if (currentUser == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Please sign in.");
            return;
        }

        String path = request.getServletPath();
        String action = request.getParameter("action");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(request.getHeader("X-Requested-With"))
                || (request.getHeader("Accept") != null && request.getHeader("Accept").contains("application/json"));

        // 1. Delete Attachment Handler
        if ("/attachment/delete".equals(path) || "delete".equalsIgnoreCase(action)) {
            handleDelete(request, response, currentUser, session, isAjax);
            return;
        }

        // 2. Upload Attachment Handler
        handleUpload(request, response, currentUser, session, isAjax);
    }

    private void handleUpload(HttpServletRequest request, HttpServletResponse response, User currentUser,
                              HttpSession session, boolean isAjax) throws IOException, ServletException {
        int taskId = ValidationUtil.parsePositiveInt(request.getParameter("taskId"), 0);
        if (taskId <= 0) {
            sendResponse(response, session, isAjax, false, "Invalid task ID for upload.", "/dashboard");
            return;
        }

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() <= 0) {
            sendResponse(response, session, isAjax, false, "Please select a file to upload.", "/tasks");
            return;
        }

        String submittedFileName = filePart.getSubmittedFileName();

        try {
            TaskAttachment saved = attachmentService.uploadAttachment(
                    taskId,
                    currentUser,
                    submittedFileName,
                    filePart.getContentType(),
                    filePart.getSize(),
                    filePart.getInputStream(),
                    getStorageDirectory()
            );

            if (isAjax) {
                JsonObject json = new JsonObject();
                json.addProperty("success", true);
                json.addProperty("message", "File uploaded successfully.");
                json.addProperty("id", saved.getId());
                json.addProperty("fileName", saved.getFileName());
                json.addProperty("fileSize", saved.getFormattedFileSize());
                json.addProperty("fileIcon", saved.getFileIconClass());
                json.addProperty("uploaderName", currentUser.getFullName());
                json.addProperty("uploadedAt", "Just now");
                json.addProperty("canDelete", true);

                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(gson.toJson(json));
            } else {
                session.setAttribute("flash_success", "File \"" + saved.getFileName() + "\" uploaded successfully.");
                String redirect = request.getHeader("Referer");
                response.sendRedirect(redirect != null ? redirect : (request.getContextPath() + "/dashboard"));
            }
        } catch (AppException e) {
            sendResponse(response, session, isAjax, false, e.getMessage(), "/tasks");
        } catch (Exception e) {
            getServletContext().log("Unexpected error uploading attachment for task " + taskId, e);
            sendResponse(response, session, isAjax, false, "An unexpected error occurred while uploading the file.", "/tasks");
        }
    }

    private void handleDelete(HttpServletRequest request, HttpServletResponse response, User currentUser,
                              HttpSession session, boolean isAjax) throws IOException {
        int attachmentId = ValidationUtil.parsePositiveInt(request.getParameter("attachmentId"), 0);
        if (attachmentId <= 0) {
            attachmentId = ValidationUtil.parsePositiveInt(request.getParameter("id"), 0);
        }
        if (attachmentId <= 0) {
            sendResponse(response, session, isAjax, false, "Invalid attachment ID.", "/tasks");
            return;
        }

        try {
            attachmentService.deleteAttachment(attachmentId, currentUser, getStorageDirectory());
            sendResponse(response, session, isAjax, true, "Attachment removed successfully.", "/tasks");
        } catch (AppException e) {
            sendResponse(response, session, isAjax, false, e.getMessage(), "/tasks");
        } catch (Exception e) {
            getServletContext().log("Error deleting attachment " + attachmentId, e);
            sendResponse(response, session, isAjax, false, "Failed to remove attachment.", "/tasks");
        }
    }

    private void sendResponse(HttpServletResponse response, HttpSession session, boolean isAjax,
                              boolean success, String message, String defaultRedirect) throws IOException {
        if (isAjax) {
            response.setStatus(success ? HttpServletResponse.SC_OK : HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json;charset=UTF-8");
            JsonObject json = new JsonObject();
            json.addProperty("success", success);
            json.addProperty(success ? "message" : "error", message);
            response.getWriter().write(gson.toJson(json));
        } else {
            if (success) {
                session.setAttribute("flash_success", message);
            } else {
                session.setAttribute("flash_error", message);
            }
            response.sendRedirect(defaultRedirect);
        }
    }
}
