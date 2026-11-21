package com.protrack.service;

import com.protrack.dao.ActivityDAO;
import com.protrack.dao.AttachmentDAO;
import com.protrack.dao.ProjectDAO;
import com.protrack.dao.TaskDAO;
import com.protrack.exception.AppException;
import com.protrack.exception.UnauthorizedAccessException;
import com.protrack.model.Project;
import com.protrack.model.Role;
import com.protrack.model.Task;
import com.protrack.model.TaskAttachment;
import com.protrack.model.TaskPriority;
import com.protrack.model.TaskStatus;
import com.protrack.model.User;
import com.protrack.model.UserFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AttachmentServiceTest {

    private AttachmentDAO mockAttachmentDAO;
    private TaskDAO mockTaskDAO;
    private ProjectDAO mockProjectDAO;
    private ActivityDAO mockActivityDAO;
    private AttachmentService attachmentService;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        mockAttachmentDAO = mock(AttachmentDAO.class);
        mockTaskDAO = mock(TaskDAO.class);
        mockProjectDAO = mock(ProjectDAO.class);
        mockActivityDAO = mock(ActivityDAO.class);

        attachmentService = new AttachmentService(mockAttachmentDAO, mockTaskDAO, mockProjectDAO, mockActivityDAO);
    }

    private Task createSampleTask(int taskId, int projectId, Integer assigneeId) {
        return new Task(
                taskId,
                "Implement Feature",
                "Task description",
                projectId,
                assigneeId,
                TaskPriority.HIGH,
                TaskStatus.IN_PROGRESS,
                Date.valueOf("2026-03-01"),
                Date.valueOf("2026-03-15"),
                null,
                16.0,
                8.0
        );
    }

    @Test
    @DisplayName("Verify filename sanitization prevents path traversal and cleans dangerous characters")
    public void testSanitizeFileName() {
        assertEquals("report.pdf", AttachmentService.sanitizeFileName("../../etc/report.pdf"));
        assertEquals("report.pdf", AttachmentService.sanitizeFileName("..\\..\\Windows\\report.pdf"));
        assertEquals("invoice.docx", AttachmentService.sanitizeFileName("/var/uploads/invoice.docx"));
        assertEquals("unnamed_file", AttachmentService.sanitizeFileName(null));
        assertEquals("unnamed_file", AttachmentService.sanitizeFileName("   "));
        assertEquals("safe.png", AttachmentService.sanitizeFileName(".../safe.png"));
    }

    @Test
    @DisplayName("Verify allowed extensions whitelist and dangerous extension rejection")
    public void testExtensionValidation() {
        assertTrue(AttachmentService.isAllowedExtension("spec.pdf"));
        assertTrue(AttachmentService.isAllowedExtension("screenshot.png"));
        assertTrue(AttachmentService.isAllowedExtension("photo.jpg"));
        assertTrue(AttachmentService.isAllowedExtension("document.docx"));
        assertTrue(AttachmentService.isAllowedExtension("data.xlsx"));
        assertTrue(AttachmentService.isAllowedExtension("schema.sql"));
        assertTrue(AttachmentService.isAllowedExtension("archive.zip"));

        assertFalse(AttachmentService.isAllowedExtension("malicious.jsp"));
        assertFalse(AttachmentService.isAllowedExtension("malicious.jspx"));
        assertFalse(AttachmentService.isAllowedExtension("hack.sh"));
        assertFalse(AttachmentService.isAllowedExtension("virus.exe"));
        assertFalse(AttachmentService.isAllowedExtension("script.php"));
        assertFalse(AttachmentService.isAllowedExtension("trojan.war"));
        assertFalse(AttachmentService.isAllowedExtension("binary.bin"));
        assertFalse(AttachmentService.isAllowedExtension(null));
    }

    @Test
    @DisplayName("Verify upload rejects empty file or file exceeding 10MB limit")
    public void testUploadFileSizeValidation() {
        User admin = UserFactory.createUser(1, "Admin", "admin@protrack.com", "hash", Role.ADMIN, "ACTIVE");

        // 0 size
        AppException exEmpty = assertThrows(AppException.class, () -> {
            attachmentService.uploadAttachment(10, admin, "test.pdf", "application/pdf", 0, new ByteArrayInputStream(new byte[0]), tempDir.toString());
        });
        assertTrue(exEmpty.getMessage().contains("Cannot upload an empty file"));

        // > 10MB
        long oversized = 10 * 1024 * 1024 + 1;
        AppException exOver = assertThrows(AppException.class, () -> {
            attachmentService.uploadAttachment(10, admin, "big.pdf", "application/pdf", oversized, new ByteArrayInputStream(new byte[1]), tempDir.toString());
        });
        assertTrue(exOver.getMessage().contains("exceeds the 10 MB limit"));
    }

    @Test
    @DisplayName("Verify upload rejects disallowed file types")
    public void testUploadDisallowedFileType() {
        User admin = UserFactory.createUser(1, "Admin", "admin@protrack.com", "hash", Role.ADMIN, "ACTIVE");

        AppException ex = assertThrows(AppException.class, () -> {
            attachmentService.uploadAttachment(10, admin, "shell.jsp", "text/plain", 50, new ByteArrayInputStream("hello".getBytes()), tempDir.toString());
        });
        assertTrue(ex.getMessage().contains("File type not allowed"));
        verify(mockAttachmentDAO, never()).save(any());
    }

    @Test
    @DisplayName("Verify successful file upload saves file to disk and records attachment in DB")
    public void testSuccessfulUpload() throws AppException {
        User admin = UserFactory.createUser(1, "Admin", "admin@protrack.com", "hash", Role.ADMIN, "ACTIVE");
        Task task = createSampleTask(10, 100, 2);
        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(task));
        when(mockAttachmentDAO.save(any())).thenAnswer(invocation -> {
            TaskAttachment saved = invocation.getArgument(0);
            saved.setId(55);
            return saved;
        });

        byte[] content = "Hello attachment content".getBytes(StandardCharsets.UTF_8);
        ByteArrayInputStream input = new ByteArrayInputStream(content);

        TaskAttachment result = attachmentService.uploadAttachment(
                10,
                admin,
                "project_spec.pdf",
                "application/pdf",
                content.length,
                input,
                tempDir.toString()
        );

        assertNotNull(result);
        assertEquals(55, result.getId());
        assertEquals("project_spec.pdf", result.getFileName());
        assertEquals(content.length, result.getFileSize());
        assertTrue(new File(result.getFilePath()).exists());

        verify(mockAttachmentDAO, times(1)).save(any(TaskAttachment.class));
        verify(mockActivityDAO, times(1)).log(eq(1), eq("TASK_ATTACHMENT_UPLOAD"), contains("project_spec.pdf"), eq("TASK"), eq(10));
    }

    @Test
    @DisplayName("Verify RBAC prevents unauthorized team member from downloading or viewing attachments")
    public void testMemberUnauthorizedAccess() {
        User member = UserFactory.createUser(99, "Other Dev", "other@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");
        Task task = createSampleTask(10, 100, 5); // assigned to user 5

        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(task));
        when(mockProjectDAO.isMemberAssigned(100, 99)).thenReturn(false);

        assertThrows(UnauthorizedAccessException.class, () -> {
            attachmentService.getAttachmentsByTask(10, member);
        });
    }

    @Test
    @DisplayName("Verify assigned team member can view and download attachments")
    public void testAssignedMemberCanAccess() throws AppException {
        User member = UserFactory.createUser(5, "Assigned Dev", "dev@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");
        Task task = createSampleTask(10, 100, 5); // assigned to member 5

        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(task));
        TaskAttachment att = new TaskAttachment(1, 10, 5, "spec.pdf", tempDir.resolve("spec.pdf").toString(), 1024L, "application/pdf", null);
        when(mockAttachmentDAO.findByTaskId(10)).thenReturn(List.of(att));

        List<TaskAttachment> list = attachmentService.getAttachmentsByTask(10, member);
        assertEquals(1, list.size());
        assertEquals("spec.pdf", list.get(0).getFileName());
    }

    @Test
    @DisplayName("Verify PM can access attachments only on projects they manage")
    public void testPMAccessControl() throws AppException {
        User pm = UserFactory.createUser(20, "Manager Sarah", "pm@protrack.com", "hash", Role.PROJECT_MANAGER, "ACTIVE");
        Task taskManaged = createSampleTask(10, 100, 5);
        Task taskNotManaged = createSampleTask(20, 200, 5);

        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(taskManaged));
        when(mockTaskDAO.findById(20)).thenReturn(Optional.of(taskNotManaged));

        Project p1 = new Project(100, "Managed", "desc", null, null, 20, "IN_PROGRESS");
        Project p2 = new Project(200, "Other", "desc", null, null, 999, "IN_PROGRESS");
        when(mockProjectDAO.findById(100)).thenReturn(Optional.of(p1));
        when(mockProjectDAO.findById(200)).thenReturn(Optional.of(p2));

        // Can access managed
        assertDoesNotThrow(() -> attachmentService.getAttachmentsByTask(10, pm));

        // Denied on unmanaged project
        assertThrows(UnauthorizedAccessException.class, () -> {
            attachmentService.getAttachmentsByTask(20, pm);
        });
    }

    @Test
    @DisplayName("Verify delete removes file from storage and database, rejecting unauthorized attempts")
    public void testDeleteAttachment() throws Exception {
        User author = UserFactory.createUser(5, "Author", "author@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");
        User stranger = UserFactory.createUser(99, "Stranger", "stranger@protrack.com", "hash", Role.TEAM_MEMBER, "ACTIVE");

        File fileOnDisk = tempDir.resolve("to_delete.png").toFile();
        assertTrue(fileOnDisk.createNewFile());

        Task task = createSampleTask(10, 100, 5);
        TaskAttachment att = new TaskAttachment(77, 10, 5, "to_delete.png", fileOnDisk.getAbsolutePath(), 500L, "image/png", null);

        when(mockAttachmentDAO.findById(77)).thenReturn(Optional.of(att));
        when(mockTaskDAO.findById(10)).thenReturn(Optional.of(task));
        when(mockProjectDAO.isMemberAssigned(100, 99)).thenReturn(false);
        when(mockAttachmentDAO.deleteById(77)).thenReturn(true);

        // Stranger denied
        assertThrows(UnauthorizedAccessException.class, () -> {
            attachmentService.deleteAttachment(77, stranger);
        });
        assertTrue(fileOnDisk.exists());

        // Author can delete
        boolean deleted = attachmentService.deleteAttachment(77, author);
        assertTrue(deleted);
        assertFalse(fileOnDisk.exists(), "File should be deleted from disk");
        verify(mockAttachmentDAO, times(1)).deleteById(77);
    }
}
