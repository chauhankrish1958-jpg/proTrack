<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!-- =========================================================================
     MODAL: Task Attachments & File Manager
     ========================================================================= -->
<div class="modal fade" id="taskAttachmentsModal" tabindex="-1" aria-labelledby="taskAttachmentsModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered modal-lg">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-light">
                <div class="d-flex align-items-center gap-2">
                    <div class="rounded-3 p-2 bg-primary-subtle text-primary">
                        <i class="bi bi-paperclip fs-5"></i>
                    </div>
                    <div>
                        <h5 class="modal-title fw-bold mb-0" id="taskAttachmentsModalLabel">Task Attachments</h5>
                        <small class="text-muted" id="attachmentTaskSubtitle">Specifications, screenshots, error logs &amp; deliverables</small>
                    </div>
                </div>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>

            <div class="modal-body p-4">
                <!-- Upload Drop Zone / Form -->
                <div class="card border border-2 border-dashed bg-light mb-4 text-center p-4 rounded-3" id="attachmentDropZone">
                    <form id="attachmentUploadForm" enctype="multipart/form-data" class="d-flex flex-column align-items-center">
                        <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                        <input type="hidden" id="uploadModalTaskId" name="taskId" value="">
                        
                        <div class="mb-2">
                            <i class="bi bi-cloud-arrow-up-fill text-primary" style="font-size: 2.5rem;"></i>
                        </div>
                        <h6 class="fw-semibold text-dark mb-1">Upload Task File or Document</h6>
                        <p class="small text-muted mb-3" style="max-width: 450px;">
                            Supported formats: <strong>PDF, PNG, JPG, DOCX, XLSX, TXT, CSV, SQL, ZIP</strong> (Max 10 MB per file)
                        </p>

                        <div class="input-group input-group-sm" style="max-width: 480px;">
                            <input type="file" class="form-control" id="attachmentFileInput" name="file" required>
                            <button type="submit" class="btn btn-primary px-3 fw-semibold" id="btnUploadAttachment">
                                <i class="bi bi-upload me-1"></i> Upload
                            </button>
                        </div>
                        <div id="attachmentUploadSpinner" class="d-none mt-2 small text-primary">
                            <div class="spinner-border spinner-border-sm me-1" role="status"></div> Uploading file, please wait...
                        </div>
                    </form>
                </div>

                <!-- Alert Message inside Modal -->
                <div id="attachmentAlert" class="alert alert-dismissible fade show d-none small mb-3" role="alert">
                    <span id="attachmentAlertText"></span>
                    <button type="button" class="btn-close" onclick="document.getElementById('attachmentAlert').classList.add('d-none')"></button>
                </div>

                <!-- Attachments List Container -->
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <h6 class="fw-bold text-dark mb-0">
                        <i class="bi bi-files me-1 text-secondary"></i> Attached Files (<span id="attachmentCountBadge">0</span>)
                    </h6>
                    <button type="button" class="btn btn-sm btn-outline-secondary" onclick="reloadTaskAttachments()" title="Refresh List">
                        <i class="bi bi-arrow-clockwise"></i> Refresh
                    </button>
                </div>

                <!-- Attachments Table / List -->
                <div class="table-responsive border rounded-3 bg-white" style="max-height: 280px; overflow-y: auto;">
                    <table class="table table-hover align-middle mb-0">
                        <thead class="table-light small">
                            <tr>
                                <th>File Name</th>
                                <th>Size</th>
                                <th>Uploaded By</th>
                                <th>Date</th>
                                <th class="text-end">Actions</th>
                            </tr>
                        </thead>
                        <tbody id="attachmentTableBody">
                            <tr>
                                <td colspan="5" class="text-center py-4 text-muted">
                                    <div class="spinner-border spinner-border-sm text-secondary me-1"></div> Loading attachments...
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>

            <div class="modal-footer bg-light">
                <button type="button" class="btn btn-secondary px-4" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>
