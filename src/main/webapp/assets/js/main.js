/**
 * ProTrack Enterprise - Frontend JavaScript Controller
 * Handles Modals, Quick Actions, Client Search, Viva Credentials Auto-fill & Chart.js Visualizations
 */

document.addEventListener('DOMContentLoaded', () => {

    // 1. Mobile Sidebar Toggle
    const sidebarToggle = document.getElementById('sidebarToggle');
    const sidebar = document.querySelector('.sidebar');
    if (sidebarToggle && sidebar) {
        sidebarToggle.addEventListener('click', () => {
            sidebar.classList.toggle('show');
        });
    }

    // 2. Auto-dismiss Alert Banners after 5 seconds
    const alerts = document.querySelectorAll('.alert-dismissible');
    alerts.forEach(alert => {
        setTimeout(() => {
            const bsAlert = bootstrap.Alert.getOrCreateInstance(alert);
            if (bsAlert) bsAlert.close();
        }, 5000);
    });

    // 3. Client-side Instant Table Search
    const searchInputs = document.querySelectorAll('.table-search-input');
    searchInputs.forEach(input => {
        input.addEventListener('keyup', (e) => {
            const query = e.target.value.toLowerCase().trim();
            const targetTableId = input.getAttribute('data-target');
            const targetTable = document.getElementById(targetTableId);
            if (!targetTable) return;

            const rows = targetTable.querySelectorAll('tbody tr');
            rows.forEach(row => {
                const text = row.innerText.toLowerCase();
                row.style.display = text.includes(query) ? '' : 'none';
            });
        });
    });

    // 4. Quick-fill Demo Credentials (For Academic / Viva Demonstrations)
    window.fillCredentials = function(email, password) {
        const emailInput = document.getElementById('loginEmail');
        const passInput = document.getElementById('loginPassword');
        if (emailInput && passInput) {
            emailInput.value = email;
            passInput.value = password;
            // Highlight briefly
            emailInput.classList.add('is-valid');
            passInput.classList.add('is-valid');
            setTimeout(() => {
                emailInput.classList.remove('is-valid');
                passInput.classList.remove('is-valid');
            }, 1000);
        }
    };

    // 5. User Edit Modal Population
    const editUserModal = document.getElementById('editUserModal');
    if (editUserModal) {
        editUserModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            const id = button.getAttribute('data-id');
            const name = button.getAttribute('data-name');
            const email = button.getAttribute('data-email');
            const role = button.getAttribute('data-role');
            const status = button.getAttribute('data-status');

            document.getElementById('editUserId').value = id || '';
            document.getElementById('editUserName').value = name || '';
            document.getElementById('editUserEmail').value = email || '';
            document.getElementById('editUserRole').value = role || 'TEAM_MEMBER';
            document.getElementById('editUserStatus').value = status || 'ACTIVE';
        });
    }

    // 5b. Change Role Modal Population
    const changeRoleModal = document.getElementById('changeRoleModal');
    if (changeRoleModal) {
        changeRoleModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            const id = button.getAttribute('data-id');
            const name = button.getAttribute('data-name');
            const email = button.getAttribute('data-email');
            const role = button.getAttribute('data-role');

            const idInput = document.getElementById('changeRoleUserId');
            const nameElem = document.getElementById('changeRoleUserName');
            const roleSelect = document.getElementById('changeRoleSelect');

            if (idInput) idInput.value = id || '';
            if (nameElem) nameElem.textContent = (name || 'User') + (email ? ' (' + email + ')' : '');
            if (roleSelect) roleSelect.value = role || 'TEAM_MEMBER';
        });
    }

    // 6. Project Edit Modal Population
    const editProjectModal = document.getElementById('editProjectModal');
    if (editProjectModal) {
        editProjectModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            document.getElementById('editProjectId').value = button.getAttribute('data-id') || '';
            document.getElementById('editProjectTitle').value = button.getAttribute('data-title') || '';
            document.getElementById('editProjectDesc').value = button.getAttribute('data-desc') || '';
            document.getElementById('editProjectStartDate').value = button.getAttribute('data-start') || '';
            document.getElementById('editProjectEndDate').value = button.getAttribute('data-end') || '';

            const managerSelect = document.getElementById('editProjectManager');
            if (managerSelect) {
                managerSelect.value = button.getAttribute('data-manager') || '';
            }

            const statusSelect = document.getElementById('editProjectStatus');
            if (statusSelect) {
                statusSelect.value = button.getAttribute('data-status') || 'PLANNING';
            }
        });
    }

    // 7. Task Edit Modal Population
    const editTaskModal = document.getElementById('editTaskModal');
    if (editTaskModal) {
        editTaskModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            document.getElementById('editTaskId').value = button.getAttribute('data-id') || '';
            document.getElementById('editTaskTitle').value = button.getAttribute('data-title') || '';
            document.getElementById('editTaskDesc').value = button.getAttribute('data-desc') || '';
            document.getElementById('editTaskProject').value = button.getAttribute('data-project') || '';

            const assigneeSelect = document.getElementById('editTaskAssignee');
            if (assigneeSelect) {
                assigneeSelect.value = button.getAttribute('data-assigned') || '';
            }

            document.getElementById('editTaskPriority').value = button.getAttribute('data-priority') || 'MEDIUM';
            document.getElementById('editTaskStatus').value = button.getAttribute('data-status') || 'TODO';
            document.getElementById('editTaskStartDate').value = button.getAttribute('data-start') || '';
            document.getElementById('editTaskDueDate').value = button.getAttribute('data-due') || '';
            const estInput = document.getElementById('editTaskEstimatedHours');
            if (estInput) {
                estInput.value = button.getAttribute('data-estimated') || '0.0';
            }
        });
    }

    // 8. Quick Task Status Change Modal (Used by Team Members)
    const updateStatusModal = document.getElementById('updateStatusModal');
    if (updateStatusModal) {
        updateStatusModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            document.getElementById('statusTaskId').value = button.getAttribute('data-id') || '';
            document.getElementById('statusTaskTitle').innerText = button.getAttribute('data-title') || 'Task';
            document.getElementById('statusTaskSelect').value = button.getAttribute('data-status') || 'TODO';
        });
    }

    // 8b. Log Task Time Modal Population
    const logTimeModal = document.getElementById('logTimeModal');
    if (logTimeModal) {
        logTimeModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            const taskIdInput = document.getElementById('logTimeTaskId');
            if (taskIdInput) taskIdInput.value = button.getAttribute('data-id') || '';
            const taskTitle = button.getAttribute('data-title') || button.getAttribute('data-name') || 'Task';
            const logged = button.getAttribute('data-logged') || '0.0';
            const estimated = button.getAttribute('data-estimated') || '0.0';

            const titleEl = document.getElementById('logTimeTaskTitle');
            if (titleEl) titleEl.innerText = taskTitle;
            const infoEl = document.getElementById('logTimeTaskHoursInfo');
            if (infoEl) infoEl.innerText = 'Current: ' + logged + 'h logged / ' + estimated + 'h estimated';
        });
    }

    // 9. Generic Delete Confirmation Modal
    const deleteModal = document.getElementById('deleteConfirmModal');
    if (deleteModal) {
        deleteModal.addEventListener('show.bs.modal', (event) => {
            const button = event.relatedTarget;
            if (!button) return;

            const id = button.getAttribute('data-id');
            const name = button.getAttribute('data-name') || 'this item';
            const actionUrl = button.getAttribute('data-action');

            document.getElementById('deleteItemName').innerText = name;
            const deleteForm = document.getElementById('deleteForm');
            if (deleteForm) {
                if (actionUrl) deleteForm.action = actionUrl;
                const idInput = deleteForm.querySelector('input[name="id"], input[name="userId"], input[name="projectId"], input[name="taskId"]');
                if (idInput) idInput.value = id;
            }
        });
    }

    // 10. Task View Mode (Table vs Kanban) Restoration
    const savedTaskView = localStorage.getItem('protrack_task_view');
    if (savedTaskView === 'kanban') {
        const kanbanBtn = document.getElementById('btnViewKanban');
        if (kanbanBtn) {
            switchTaskView('kanban');
        }
    }
});

/**
 * Chart Initialization Helper Functions using Chart.js
 */
window.initAdminCharts = function(statusData, priorityData) {
    // 1. Project Status Doughnut Chart
    const statusCtx = document.getElementById('projectStatusChart');
    if (statusCtx && statusData) {
        new Chart(statusCtx, {
            type: 'doughnut',
            data: {
                labels: ['Planning', 'In Progress', 'On Hold', 'Completed', 'Cancelled'],
                datasets: [{
                    data: [
                        statusData.PLANNING || 0,
                        statusData.IN_PROGRESS || 0,
                        statusData.ON_HOLD || 0,
                        statusData.COMPLETED || 0,
                        statusData.CANCELLED || 0
                    ],
                    backgroundColor: [
                        '#0ea5e9', // Planning (Sky blue)
                        '#6366f1', // In Progress (Indigo)
                        '#f59e0b', // On Hold (Amber)
                        '#10b981', // Completed (Emerald)
                        '#ef4444'  // Cancelled (Red)
                    ],
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: { boxWidth: 12, padding: 15, font: { family: 'Inter', size: 12 } }
                    }
                },
                cutout: '65%'
            }
        });
    }

    // 2. Task Priority Bar Chart
    const priorityCtx = document.getElementById('taskPriorityChart');
    if (priorityCtx && priorityData) {
        new Chart(priorityCtx, {
            type: 'bar',
            data: {
                labels: ['Low', 'Medium', 'High', 'Critical'],
                datasets: [{
                    label: 'Tasks',
                    data: [
                        priorityData.LOW || 0,
                        priorityData.MEDIUM || 0,
                        priorityData.HIGH || 0,
                        priorityData.CRITICAL || 0
                    ],
                    backgroundColor: [
                        'rgba(14, 165, 233, 0.85)',
                        'rgba(100, 116, 139, 0.85)',
                        'rgba(245, 158, 11, 0.85)',
                        'rgba(239, 68, 68, 0.85)'
                    ],
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: { display: false }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        ticks: { stepSize: 1, font: { family: 'Inter' } },
                        grid: { color: '#f1f5f9' }
                    },
                    x: {
                        grid: { display: false },
                        ticks: { font: { family: 'Inter' } }
                    }
                }
            }
        });
    }
};

/* ==========================================================================
   Kanban Board View & Drag-and-Drop Controller
   ========================================================================== */

/**
 * Switches between Table (List) and Kanban Board view with localStorage persistence.
 */
window.switchTaskView = function(viewMode) {
    const tableView = document.getElementById('tasksTableViewCard');
    const kanbanView = document.getElementById('kanbanBoardView');
    const btnTable = document.getElementById('btnViewTable');
    const btnKanban = document.getElementById('btnViewKanban');

    if (!tableView || !kanbanView) return;

    if (viewMode === 'kanban') {
        tableView.classList.add('d-none');
        kanbanView.classList.remove('d-none');
        if (btnTable) btnTable.classList.remove('active');
        if (btnKanban) btnKanban.classList.add('active');
        localStorage.setItem('protrack_task_view', 'kanban');
    } else {
        kanbanView.classList.add('d-none');
        tableView.classList.remove('d-none');
        if (btnKanban) btnKanban.classList.remove('active');
        if (btnTable) btnTable.classList.add('active');
        localStorage.setItem('protrack_task_view', 'table');
    }
};

/**
 * Drag and Drop Handlers for Kanban Cards
 */
window.handleKanbanDragStart = function(event) {
    const card = event.currentTarget;
    card.classList.add('is-dragging');
    event.dataTransfer.effectAllowed = 'move';
    event.dataTransfer.setData('text/plain', card.getAttribute('data-id'));
    event.dataTransfer.setData('source-status', card.getAttribute('data-status'));
    window.activeKanbanCard = card;
};

window.handleKanbanDragEnd = function(event) {
    const card = event.currentTarget;
    card.classList.remove('is-dragging');
    document.querySelectorAll('.kanban-column-body').forEach(col => {
        col.classList.remove('drag-over');
    });
    window.activeKanbanCard = null;
};

window.allowKanbanDrop = function(event) {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'move';
    const columnBody = event.currentTarget;
    if (!columnBody.classList.contains('drag-over')) {
        columnBody.classList.add('drag-over');
    }
};

window.handleKanbanDragLeave = function(event) {
    const columnBody = event.currentTarget;
    // Check if genuinely leaving the drop zone (not entering child element)
    if (!columnBody.contains(event.relatedTarget)) {
        columnBody.classList.remove('drag-over');
    }
};

window.handleKanbanDrop = function(event, targetStatus) {
    event.preventDefault();
    const columnBody = event.currentTarget;
    columnBody.classList.remove('drag-over');

    const taskId = event.dataTransfer.getData('text/plain');
    const sourceStatus = event.dataTransfer.getData('source-status');

    if (!taskId || !targetStatus || sourceStatus === targetStatus) {
        return;
    }

    const card = document.querySelector(`.kanban-card[data-id="${taskId}"]`);
    if (!card) return;

    // Execute status transition via AJAX
    updateTaskStatusAjax(taskId, targetStatus, card, sourceStatus, columnBody);
};

/**
 * Performs asynchronous task status update with CSRF token and updates UI on success
 */
window.updateTaskStatusAjax = function(taskId, targetStatus, card, sourceStatus, targetColumnBody) {
    const csrfMeta = document.querySelector('meta[name="csrf-token"]');
    const csrfToken = csrfMeta ? csrfMeta.getAttribute('content') : '';
    const endpoint = window.location.pathname;

    const formData = new URLSearchParams();
    formData.append('action', 'updateStatus');
    formData.append('taskId', taskId);
    formData.append('status', targetStatus);
    formData.append('ajax', 'true');
    if (csrfToken) {
        formData.append('csrfToken', csrfToken);
    }

    fetch(endpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest',
            'X-CSRF-TOKEN': csrfToken
        },
        body: formData.toString()
    })
    .then(async response => {
        const data = await response.json().catch(() => ({}));
        if (!response.ok) {
            throw new Error(data.error || 'Failed to update task status');
        }
        return data;
    })
    .then(data => {
        // 1. Move card to new column
        const oldColumn = card.closest('.kanban-column-body');
        card.setAttribute('data-status', targetStatus);

        // Remove empty state placeholder in target column if present
        const emptyState = targetColumnBody.querySelector('.kanban-empty-state');
        if (emptyState) emptyState.remove();

        targetColumnBody.appendChild(card);

        // 2. Update card's status badge
        const statusBadge = card.querySelector('.kanban-status-badge');
        if (statusBadge) {
            statusBadge.className = 'badge badge-custom kanban-status-badge ' + getBadgeClassForStatus(targetStatus);
            statusBadge.innerText = formatStatusLabel(targetStatus);
        }

        // 3. Update column counter badges
        refreshKanbanColumnCounters();

        // 4. Check if old column is now empty
        if (oldColumn && oldColumn.querySelectorAll('.kanban-card').length === 0) {
            const placeholder = document.createElement('div');
            placeholder.className = 'kanban-empty-state';
            placeholder.innerHTML = '<i class="bi bi-inbox"></i><span>No tasks in this stage</span>';
            oldColumn.appendChild(placeholder);
        }

        // 5. Show success notification
        showKanbanToast(data.message || `Task moved to ${formatStatusLabel(targetStatus)}`, 'success');
    })
    .catch(error => {
        console.error('Task status update failed:', error);
        showKanbanToast(error.message || 'Error updating task status', 'danger');
    });
};

/**
 * Quick status update helper for keyboard/dropdown actions on cards
 */
window.quickTransitionTaskStatus = function(taskId, newStatus) {
    const card = document.querySelector(`.kanban-card[data-id="${taskId}"]`);
    if (!card) return;
    const sourceStatus = card.getAttribute('data-status');
    const targetColumn = document.querySelector(`.kanban-column-body[data-status="${newStatus}"]`);
    if (!targetColumn) return;

    updateTaskStatusAjax(taskId, newStatus, card, sourceStatus, targetColumn);
};

function refreshKanbanColumnCounters() {
    document.querySelectorAll('.kanban-column').forEach(col => {
        const status = col.getAttribute('data-column-status');
        const count = col.querySelectorAll('.kanban-card').length;
        const badge = col.querySelector('.kanban-count-badge');
        if (badge) badge.innerText = count;
    });
}

function getBadgeClassForStatus(status) {
    switch (status) {
        case 'TODO': return 'bg-secondary';
        case 'IN_PROGRESS': return 'bg-primary';
        case 'REVIEW': return 'bg-warning text-dark';
        case 'COMPLETED': return 'bg-success';
        default: return 'bg-secondary';
    }
}

function formatStatusLabel(status) {
    switch (status) {
        case 'TODO': return 'To Do';
        case 'IN_PROGRESS': return 'In Progress';
        case 'REVIEW': return 'In Review';
        case 'COMPLETED': return 'Completed';
        default: return status;
    }
}

/**
 * Displays floating notification toast
 */
function showKanbanToast(message, type) {
    let container = document.querySelector('.kanban-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'kanban-toast-container';
        document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = 'kanban-toast';
    const icon = type === 'success' ? 'bi-check-circle-fill text-success' : 'bi-exclamation-triangle-fill text-danger';
    toast.innerHTML = `<i class="bi ${icon} fs-5"></i><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateY(12px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// =========================================================================
// 10. TASK ATTACHMENTS & DOCUMENT MANAGER CONTROLLER
// =========================================================================

function getAppContextPath() {
    const meta = document.querySelector('meta[name="context-path"]');
    return meta ? meta.getAttribute('content') : '';
}

function getAppCsrfToken() {
    const meta = document.querySelector('meta[name="csrf-token"]');
    return meta ? meta.getAttribute('content') : '';
}

function escapeHtml(text) {
    if (!text) return '';
    return text.toString().replace(/[&<>"']/g, function(m) {
        return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#039;' }[m];
    });
}

function showAttachmentAlert(message, type) {
    const alertEl = document.getElementById('attachmentAlert');
    const textEl = document.getElementById('attachmentAlertText');
    if (alertEl && textEl) {
        alertEl.className = 'alert alert-' + type + ' alert-dismissible fade show small mb-3';
        textEl.innerText = message;
        alertEl.classList.remove('d-none');
    }
}

function hideAttachmentAlert() {
    const alertEl = document.getElementById('attachmentAlert');
    if (alertEl) {
        alertEl.classList.add('d-none');
    }
}

/**
 * Opens task attachments modal and triggers AJAX reload
 */
window.openTaskAttachmentsModal = function(taskId, taskTitle) {
    if (!taskId) return;

    const taskIdInput = document.getElementById('uploadModalTaskId');
    if (taskIdInput) taskIdInput.value = taskId;

    const titleEl = document.getElementById('taskAttachmentsModalLabel');
    if (titleEl) {
        titleEl.innerHTML = 'Task Attachments <span class="badge bg-primary-subtle text-primary ms-1">#' + taskId + '</span>';
    }

    const subtitleEl = document.getElementById('attachmentTaskSubtitle');
    if (subtitleEl && taskTitle) {
        subtitleEl.innerText = taskTitle;
    }

    const fileInput = document.getElementById('attachmentFileInput');
    if (fileInput) fileInput.value = '';

    hideAttachmentAlert();

    const modalEl = document.getElementById('taskAttachmentsModal');
    if (modalEl) {
        const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
        modal.show();
    }

    reloadTaskAttachments();
};

/**
 * Loads attachments for current task ID via AJAX
 */
window.reloadTaskAttachments = function() {
    const taskIdInput = document.getElementById('uploadModalTaskId');
    const taskId = taskIdInput ? taskIdInput.value : null;
    if (!taskId) return;

    const tbody = document.getElementById('attachmentTableBody');
    const countBadge = document.getElementById('attachmentCountBadge');

    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-secondary me-1"></div> Loading attachments...</td></tr>';
    }

    const ctx = getAppContextPath();
    fetch(ctx + '/attachment?taskId=' + encodeURIComponent(taskId), {
        headers: { 'Accept': 'application/json' }
    })
    .then(async response => {
        const data = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error((data && data.error) ? data.error : 'Failed to retrieve attachments.');
        }
        return data;
    })
    .then(data => {
        if (!tbody) return;
        if (Array.isArray(data)) {
            if (countBadge) countBadge.innerText = data.length;

            if (data.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center py-4 text-muted">' +
                    '<i class="bi bi-folder2-open d-block fs-3 mb-1 text-secondary opacity-50"></i>' +
                    'No files attached to this task yet. Use the upload box above.</td></tr>';
            } else {
                tbody.innerHTML = data.map(att => {
                    return '<tr>' +
                        '<td>' +
                            '<div class="d-flex align-items-center gap-2">' +
                                '<i class="' + (att.fileIcon || 'bi bi-file-earmark') + ' fs-5 text-primary"></i>' +
                                '<span class="fw-semibold text-dark text-break" style="max-width: 220px;" title="' + escapeHtml(att.fileName) + '">' +
                                    escapeHtml(att.fileName) +
                                '</span>' +
                            '</div>' +
                        '</td>' +
                        '<td class="small text-muted text-nowrap">' + escapeHtml(att.fileSize) + '</td>' +
                        '<td class="small text-muted text-nowrap"><i class="bi bi-person me-1"></i>' + escapeHtml(att.uploaderName) + '</td>' +
                        '<td class="small text-muted text-nowrap">' + escapeHtml(att.uploadedAt) + '</td>' +
                        '<td class="text-end text-nowrap">' +
                            '<a href="' + ctx + '/attachment/download?id=' + att.id + '" class="btn btn-sm btn-outline-primary py-0 px-2 me-1" title="Download File">' +
                                '<i class="bi bi-download"></i>' +
                            '</a>' +
                            (att.canDelete ?
                            '<button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" onclick="deleteTaskAttachment(' + att.id + ')" title="Delete File">' +
                                '<i class="bi bi-trash"></i>' +
                            '</button>' : '') +
                        '</td>' +
                    '</tr>';
                }).join('');
            }
        } else {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-danger">' +
                ((data && data.error) ? escapeHtml(data.error) : 'Unable to load attachments.') + '</td></tr>';
        }
    })
    .catch(err => {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center py-3 text-danger"><i class="bi bi-exclamation-triangle me-1"></i>' +
                escapeHtml(err.message || 'Error loading attachments.') + '</td></tr>';
        }
    });
};

/**
 * Deletes attachment via AJAX with CSRF validation
 */
window.deleteTaskAttachment = function(attachmentId) {
    if (!attachmentId) return;
    if (!confirm('Are you sure you want to permanently delete this attachment? This operation cannot be undone.')) {
        return;
    }

    const ctx = getAppContextPath();
    const csrfToken = getAppCsrfToken();

    const params = new URLSearchParams();
    params.append('id', attachmentId);
    params.append('attachmentId', attachmentId);
    params.append('csrfToken', csrfToken);

    fetch(ctx + '/attachment/delete', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'X-Requested-With': 'XMLHttpRequest',
            'X-CSRF-TOKEN': csrfToken
        },
        body: params.toString()
    })
    .then(async response => {
        const data = await response.json().catch(() => null);
        if (!response.ok) {
            throw new Error((data && data.error) ? data.error : 'Failed to delete attachment.');
        }
        return data;
    })
    .then(data => {
        showAttachmentAlert(data.message || 'Attachment deleted successfully.', 'info');
        reloadTaskAttachments();
    })
    .catch(err => {
        showAttachmentAlert(err.message || 'Error removing attachment.', 'danger');
    });
};

// Wire upload form submission and drop zone
document.addEventListener('DOMContentLoaded', () => {
    const uploadForm = document.getElementById('attachmentUploadForm');
    const dropZone = document.getElementById('attachmentDropZone');
    const fileInput = document.getElementById('attachmentFileInput');

    if (uploadForm) {
        uploadForm.addEventListener('submit', (e) => {
            e.preventDefault();
            if (!fileInput || !fileInput.files || fileInput.files.length === 0) {
                showAttachmentAlert('Please select a file to upload.', 'warning');
                return;
            }

            const file = fileInput.files[0];
            if (file.size > 10 * 1024 * 1024) {
                showAttachmentAlert('File exceeds the 10 MB limit (' + (file.size / (1024 * 1024)).toFixed(1) + ' MB).', 'danger');
                return;
            }

            const taskId = document.getElementById('uploadModalTaskId').value;
            const csrfToken = getAppCsrfToken();
            const ctx = getAppContextPath();

            const formData = new FormData();
            formData.append('taskId', taskId);
            formData.append('file', file);
            formData.append('csrfToken', csrfToken);

            const uploadBtn = document.getElementById('btnUploadAttachment');
            const spinner = document.getElementById('attachmentUploadSpinner');

            if (uploadBtn) uploadBtn.disabled = true;
            if (spinner) spinner.classList.remove('d-none');
            hideAttachmentAlert();

            fetch(ctx + '/attachment', {
                method: 'POST',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: formData
            })
            .then(async response => {
                const data = await response.json().catch(() => null);
                if (!response.ok) {
                    throw new Error((data && data.error) ? data.error : 'Failed to upload attachment.');
                }
                return data;
            })
            .then(data => {
                if (fileInput) fileInput.value = '';
                showAttachmentAlert(data.message || 'File uploaded successfully!', 'success');
                reloadTaskAttachments();
            })
            .catch(err => {
                showAttachmentAlert(err.message || 'File upload failed.', 'danger');
            })
            .finally(() => {
                if (uploadBtn) uploadBtn.disabled = false;
                if (spinner) spinner.classList.add('d-none');
            });
        });
    }

    if (dropZone && fileInput) {
        ['dragenter', 'dragover'].forEach(eventName => {
            dropZone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropZone.classList.add('border-primary', 'bg-primary-subtle');
            }, false);
        });

        ['dragleave', 'drop'].forEach(eventName => {
            dropZone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                dropZone.classList.remove('border-primary', 'bg-primary-subtle');
            }, false);
        });

        dropZone.addEventListener('drop', (e) => {
            if (e.dataTransfer && e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                fileInput.files = e.dataTransfer.files;
            }
        }, false);
    }
});

