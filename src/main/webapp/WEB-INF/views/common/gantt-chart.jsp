<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.time.LocalDate" %>
<%@ page import="java.time.format.DateTimeFormatter" %>
<%@ page import="java.time.temporal.ChronoUnit" %>
<%@ page import="com.protrack.model.Project" %>
<%@ page import="com.protrack.model.Task" %>
<%@ page import="com.protrack.model.TaskStatus" %>
<%@ page import="com.protrack.model.TaskPriority" %>
<%@ page import="com.protrack.util.DateUtil" %>
<%@ page import="com.protrack.util.ValidationUtil" %>

<%
    Project ganttProject = (Project) request.getAttribute("project");
    @SuppressWarnings("unchecked")
    List<Task> ganttTasks = (List<Task>) request.getAttribute("tasks");
    if (ganttTasks == null) {
        ganttTasks = new ArrayList<>();
    }

    LocalDate today = LocalDate.now();

    // 1. Calculate timeline bounds across project and all tasks
    LocalDate minDate = (ganttProject != null && ganttProject.getStartDate() != null)
            ? ganttProject.getStartDate().toLocalDate() : today.minusDays(5);
    LocalDate maxDate = (ganttProject != null && ganttProject.getEndDate() != null)
            ? ganttProject.getEndDate().toLocalDate() : today.plusDays(25);

    for (Task t : ganttTasks) {
        if (t.getStartDate() != null && t.getStartDate().toLocalDate().isBefore(minDate)) {
            minDate = t.getStartDate().toLocalDate();
        }
        if (t.getDueDate() != null && t.getDueDate().toLocalDate().isAfter(maxDate)) {
            maxDate = t.getDueDate().toLocalDate();
        }
    }

    // Add visual breathing room margins
    minDate = minDate.minusDays(2);
    maxDate = maxDate.plusDays(4);

    long totalDays = ChronoUnit.DAYS.between(minDate, maxDate) + 1;
    if (totalDays < 14) {
        maxDate = minDate.plusDays(14);
        totalDays = 15;
    }

    // Check if Today falls within timeline
    boolean todayInRange = !today.isBefore(minDate) && !today.isAfter(maxDate);
    double todayPercent = todayInRange
            ? (((double) ChronoUnit.DAYS.between(minDate, today) + 0.5) / (double) totalDays) * 100.0
            : -1.0;

    // Timeline tick intervals (6 to 8 milestone columns)
    DateTimeFormatter tickMonthDay = DateTimeFormatter.ofPattern("MMM dd");
    DateTimeFormatter tickDayOfWeek = DateTimeFormatter.ofPattern("EEE");
    int tickCount = Math.min(8, Math.max(5, (int) (totalDays / 4)));
    long intervalDays = Math.max(1, totalDays / (tickCount - 1));

    int todoCount = 0;
    int inProgressCount = 0;
    int reviewCount = 0;
    int completedCount = 0;
    for (Task t : ganttTasks) {
        if (t.getStatus() == TaskStatus.TODO) todoCount++;
        else if (t.getStatus() == TaskStatus.IN_PROGRESS) inProgressCount++;
        else if (t.getStatus() == TaskStatus.REVIEW) reviewCount++;
        else if (t.getStatus() == TaskStatus.COMPLETED) completedCount++;
    }
%>

<style>
/* =========================================================================
   Interactive Gantt Timeline Component Styles
   ========================================================================= */
.gantt-wrapper {
    background: #ffffff;
    border: 1px solid #e2e8f0;
    border-radius: 12px;
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
    overflow: hidden;
    margin-bottom: 24px;
}

.gantt-toolbar {
    background-color: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    padding: 14px 20px;
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
}

.gantt-board-container {
    display: flex;
    overflow-x: auto;
    position: relative;
    min-height: 280px;
}

/* Left Sidebar: Task List */
.gantt-sidebar {
    width: 300px;
    min-width: 300px;
    border-right: 2px solid #e2e8f0;
    background: #ffffff;
    z-index: 5;
    flex-shrink: 0;
}

.gantt-sidebar-header {
    height: 54px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: 0.8rem;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    color: #475569;
    background: #f1f5f9;
    border-bottom: 1px solid #cbd5e1;
}

.gantt-sidebar-row {
    height: 52px;
    padding: 0 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    border-bottom: 1px solid #f1f5f9;
    transition: background-color 0.15s ease;
}

.gantt-sidebar-row:hover {
    background-color: #f8fafc;
}

.gantt-sidebar-row.compact-mode {
    height: 42px;
}

/* Right Main: Timeline Chart Area */
.gantt-timeline-area {
    flex-grow: 1;
    min-width: 750px;
    position: relative;
    background: #ffffff;
}

/* Timeline Column Header */
.gantt-timeline-header {
    height: 54px;
    position: relative;
    background: #f8fafc;
    border-bottom: 1px solid #cbd5e1;
}

.gantt-tick-marker {
    position: absolute;
    top: 0;
    bottom: 0;
    border-left: 1px dashed #e2e8f0;
    padding-left: 6px;
    padding-top: 8px;
    font-size: 0.72rem;
    color: #64748b;
    font-weight: 600;
    pointer-events: none;
    white-space: nowrap;
}

/* Today Vertical Indicator Line */
.gantt-today-line {
    position: absolute;
    top: 0;
    bottom: 0;
    width: 2px;
    background-color: #ef4444;
    z-index: 4;
    pointer-events: none;
    box-shadow: 0 0 6px rgba(239, 68, 68, 0.4);
}

.gantt-today-badge {
    position: absolute;
    top: 4px;
    left: -22px;
    background-color: #ef4444;
    color: #ffffff;
    font-size: 0.65rem;
    font-weight: 800;
    padding: 2px 6px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.05em;
    box-shadow: 0 2px 4px rgba(239, 68, 68, 0.3);
}

/* Timeline Rows & Task Bars */
.gantt-chart-row {
    height: 52px;
    position: relative;
    border-bottom: 1px solid #f1f5f9;
    transition: background-color 0.15s ease;
}

.gantt-chart-row:hover {
    background-color: #f8fafc;
}

.gantt-chart-row.compact-mode {
    height: 42px;
}

.gantt-grid-line {
    position: absolute;
    top: 0;
    bottom: 0;
    border-left: 1px dashed #f1f5f9;
    pointer-events: none;
}

.gantt-bar-item {
    position: absolute;
    top: 10px;
    height: 32px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    padding: 0 10px;
    color: #ffffff;
    font-size: 0.75rem;
    font-weight: 600;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    cursor: pointer;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    z-index: 2;
}

.gantt-bar-item:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
    z-index: 10;
}

.gantt-chart-row.compact-mode .gantt-bar-item {
    top: 8px;
    height: 26px;
    font-size: 0.7rem;
}

/* Status Specific Colors */
.gantt-bar-todo {
    background: linear-gradient(135deg, #64748b 0%, #475569 100%);
    border: 1px solid #334155;
}

.gantt-bar-inprogress {
    background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
    border: 1px solid #1d4ed8;
}

.gantt-bar-review {
    background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
    border: 1px solid #b45309;
}

.gantt-bar-completed {
    background: linear-gradient(135deg, #10b981 0%, #059669 100%);
    border: 1px solid #047857;
}

/* Overdue Stripes Indicator */
.gantt-bar-overdue {
    background-image: repeating-linear-gradient(
        45deg,
        rgba(239, 68, 68, 0.85),
        rgba(239, 68, 68, 0.85) 10px,
        rgba(220, 38, 38, 0.95) 10px,
        rgba(220, 38, 38, 0.95) 20px
    ) !important;
    border: 1px solid #b91c1c !important;
}

/* Internal Progress Fill Bar */
.gantt-bar-progress {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    background: rgba(255, 255, 255, 0.22);
    border-radius: 5px 0 0 5px;
    pointer-events: none;
}

.gantt-bar-content {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 6px;
    width: 100%;
}

.gantt-legend {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 16px;
    font-size: 0.75rem;
    color: #64748b;
    padding: 12px 20px;
    background-color: #f8fafc;
    border-top: 1px solid #e2e8f0;
}

.gantt-legend-item {
    display: flex;
    align-items: center;
    gap: 6px;
}

.gantt-legend-color {
    width: 14px;
    height: 14px;
    border-radius: 4px;
    display: inline-block;
}
</style>

<div class="gantt-wrapper">
    <!-- 1. Interactive Toolbar -->
    <div class="gantt-toolbar">
        <div class="d-flex flex-wrap align-items-center gap-2">
            <!-- Status Filter Pills -->
            <div class="btn-group btn-group-sm" role="group" aria-label="Status Filter">
                <button type="button" class="btn btn-outline-secondary active fw-semibold" onclick="filterGanttStatus('ALL', this)">
                    All (<%= ganttTasks.size() %>)
                </button>
                <button type="button" class="btn btn-outline-secondary fw-semibold" onclick="filterGanttStatus('TODO', this)">
                    To Do (<%= todoCount %>)
                </button>
                <button type="button" class="btn btn-outline-secondary fw-semibold" onclick="filterGanttStatus('IN_PROGRESS', this)">
                    In Progress (<%= inProgressCount %>)
                </button>
                <button type="button" class="btn btn-outline-secondary fw-semibold" onclick="filterGanttStatus('REVIEW', this)">
                    Review (<%= reviewCount %>)
                </button>
                <button type="button" class="btn btn-outline-secondary fw-semibold" onclick="filterGanttStatus('COMPLETED', this)">
                    Done (<%= completedCount %>)
                </button>
            </div>

            <!-- Priority Dropdown Filter -->
            <select class="form-select form-select-sm" id="ganttPriorityFilter" onchange="filterGanttPriority(this.value)" style="width: 130px;">
                <option value="ALL">All Priority</option>
                <option value="URGENT">Urgent</option>
                <option value="HIGH">High</option>
                <option value="MEDIUM">Medium</option>
                <option value="LOW">Low</option>
            </select>
        </div>

        <div class="d-flex align-items-center gap-3">
            <!-- Live Search -->
            <div class="input-group input-group-sm" style="width: 200px;">
                <span class="input-group-text bg-white border-end-0 text-muted"><i class="bi bi-search"></i></span>
                <input type="text" id="ganttSearchInput" class="form-control border-start-0" placeholder="Filter tasks..." onkeyup="filterGanttSearch(this.value)">
            </div>

            <!-- View Mode Switch -->
            <div class="btn-group btn-group-sm">
                <button type="button" class="btn btn-outline-secondary active" id="btnGanttNormal" onclick="toggleGanttDensity(false)" title="Standard Height">
                    <i class="bi bi-view-stacked"></i>
                </button>
                <button type="button" class="btn btn-outline-secondary" id="btnGanttCompact" onclick="toggleGanttDensity(true)" title="Compact Height">
                    <i class="bi bi-distribute-vertical"></i>
                </button>
            </div>

            <!-- Milestone Date Span Badge -->
            <span class="badge bg-light text-dark border small py-2 px-3 d-none d-lg-inline-block">
                <i class="bi bi-calendar-range text-primary me-1"></i>
                <%= DateUtil.formatDisplayDate(Date.valueOf(minDate)) %> &mdash; <%= DateUtil.formatDisplayDate(Date.valueOf(maxDate)) %>
                <span class="text-muted ms-1">(<%= totalDays %> days)</span>
            </span>
        </div>
    </div>

    <!-- 2. Interactive Gantt Timeline Area -->
    <div class="gantt-board-container" id="ganttBoardContainer">
        
        <!-- Left Sidebar: Task Names & Meta -->
        <div class="gantt-sidebar">
            <div class="gantt-sidebar-header">
                <span><i class="bi bi-list-task me-1"></i> Task Schedule</span>
                <span class="badge bg-white text-secondary border"><%= ganttTasks.size() %></span>
            </div>

            <div id="ganttSidebarList">
                <% if (!ganttTasks.isEmpty()) {
                    for (Task t : ganttTasks) {
                        String titleEsc = ValidationUtil.escapeXml(t.getTaskTitle());
                        String assignee = t.getAssignedToName() != null ? ValidationUtil.escapeXml(t.getAssignedToName()) : "Unassigned";
                        String initial = t.getAssignedToName() != null && !t.getAssignedToName().isEmpty()
                                ? ValidationUtil.escapeXml(t.getAssignedToName().substring(0, 1).toUpperCase()) : "?";
                %>
                <div class="gantt-sidebar-row" data-task-id="<%= t.getId() %>" data-status="<%= t.getStatus().name() %>" data-priority="<%= t.getPriority().name() %>" data-title="<%= titleEsc.toLowerCase() %>" data-assignee="<%= assignee.toLowerCase() %>">
                    <div class="d-flex align-items-center gap-2 text-truncate me-2" style="max-width: 190px;">
                        <span class="badge rounded-pill bg-light text-secondary border fw-bold" style="font-size: 0.65rem; width: 22px; height: 22px; display: inline-flex; align-items: center; justify-content: center;">
                            <%= initial %>
                        </span>
                        <div class="text-truncate">
                            <span class="fw-semibold text-dark small text-truncate d-block" title="<%= titleEsc %>">
                                <%= titleEsc %>
                            </span>
                            <span class="text-muted" style="font-size: 0.68rem;"><%= assignee %></span>
                        </div>
                    </div>
                    <div class="d-flex align-items-center gap-1">
                        <button type="button" class="btn btn-sm btn-outline-custom py-0 px-1" title="Files & Attachments"
                                onclick="openTaskAttachmentsModal(<%= t.getId() %>, '<%= titleEsc.replace("'", "\\'") %>')">
                            <i class="bi bi-paperclip text-secondary" style="font-size: 0.75rem;"></i>
                        </button>
                        <span class="badge badge-custom <%= t.getStatus().getBadgeClass() %>" style="font-size: 0.65rem;">
                            <%= t.getStatus().getLabel() %>
                        </span>
                    </div>
                </div>
                <%   }
                   } else { %>
                <div class="text-center py-5 text-muted small">
                    <i class="bi bi-inbox fs-3 d-block text-secondary mb-2 opacity-50"></i>
                    No tasks found in project.
                </div>
                <% } %>
            </div>
        </div>

        <!-- Right Main Timeline Chart Track -->
        <div class="gantt-timeline-area" id="ganttTimelineArea">
            
            <!-- Timeline Grid Header (Date Milestone Markers) -->
            <div class="gantt-timeline-header">
                <% for (int i = 0; i < tickCount; i++) {
                    LocalDate tickDate = minDate.plusDays(i * intervalDays);
                    if (tickDate.isAfter(maxDate)) tickDate = maxDate;
                    double tickPercent = (((double) ChronoUnit.DAYS.between(minDate, tickDate)) / (double) totalDays) * 100.0;
                %>
                <div class="gantt-tick-marker" style="left: <%= String.format(java.util.Locale.US, "%.2f", tickPercent) %>%;">
                    <div><%= tickDate.format(tickMonthDay) %></div>
                    <div class="text-muted small" style="font-size: 0.65rem;"><%= tickDate.format(tickDayOfWeek) %></div>
                </div>
                <% } %>

                <!-- Today Indicator Line in Header -->
                <% if (todayInRange) { %>
                <div class="gantt-today-line" style="left: <%= String.format(java.util.Locale.US, "%.2f", todayPercent) %>%;">
                    <div class="gantt-today-badge">Today</div>
                </div>
                <% } %>
            </div>

            <!-- Task Bar Rows -->
            <div id="ganttChartRowsContainer" style="position: relative;">
                
                <!-- Full-height Today Vertical Line -->
                <% if (todayInRange) { %>
                <div class="gantt-today-line" style="left: <%= String.format(java.util.Locale.US, "%.2f", todayPercent) %>%;"></div>
                <% } %>

                <!-- Background Vertical Guide Lines -->
                <% for (int i = 0; i < tickCount; i++) {
                    LocalDate tickDate = minDate.plusDays(i * intervalDays);
                    if (tickDate.isAfter(maxDate)) tickDate = maxDate;
                    double tickPercent = (((double) ChronoUnit.DAYS.between(minDate, tickDate)) / (double) totalDays) * 100.0;
                %>
                <div class="gantt-grid-line" style="left: <%= String.format(java.util.Locale.US, "%.2f", tickPercent) %>%;"></div>
                <% } %>

                <!-- Render Each Task Gantt Bar -->
                <% if (!ganttTasks.isEmpty()) {
                    for (Task t : ganttTasks) {
                        String titleEsc = ValidationUtil.escapeXml(t.getTaskTitle());
                        String assignee = t.getAssignedToName() != null ? ValidationUtil.escapeXml(t.getAssignedToName()) : "Unassigned";

                        LocalDate taskStart = (t.getStartDate() != null) ? t.getStartDate().toLocalDate() : minDate.plusDays(2);
                        LocalDate taskEnd = (t.getDueDate() != null) ? t.getDueDate().toLocalDate() : taskStart.plusDays(3);
                        if (taskEnd.isBefore(taskStart)) {
                            taskEnd = taskStart;
                        }

                        long startOffsetDays = ChronoUnit.DAYS.between(minDate, taskStart);
                        if (startOffsetDays < 0) startOffsetDays = 0;

                        long durationDays = ChronoUnit.DAYS.between(taskStart, taskEnd) + 1;
                        if (durationDays < 1) durationDays = 1;

                        double leftPercent = ((double) startOffsetDays / (double) totalDays) * 100.0;
                        double widthPercent = ((double) durationDays / (double) totalDays) * 100.0;

                        // Ensure minimal visibility
                        if (widthPercent < 3.2) widthPercent = 3.2;
                        if (leftPercent + widthPercent > 100.0) {
                            widthPercent = Math.max(2.5, 100.0 - leftPercent);
                        }

                        // Determine bar styling
                        String barClass = "gantt-bar-todo";
                        int progressFill = 0;
                        if (t.getStatus() == TaskStatus.COMPLETED) {
                            barClass = "gantt-bar-completed";
                            progressFill = 100;
                        } else if (t.getStatus() == TaskStatus.IN_PROGRESS) {
                            barClass = "gantt-bar-inprogress";
                            progressFill = Math.max(25, t.getTimeProgressPercentage());
                        } else if (t.getStatus() == TaskStatus.REVIEW) {
                            barClass = "gantt-bar-review";
                            progressFill = 80;
                        }

                        if (t.isOverdue()) {
                            barClass += " gantt-bar-overdue";
                        }

                        String tooltipHtml = "<strong>" + titleEsc + "</strong><br>" +
                                "Assignee: " + assignee + "<br>" +
                                "Timeline: " + DateUtil.formatDisplayDate(Date.valueOf(taskStart)) + " to " + DateUtil.formatDisplayDate(Date.valueOf(taskEnd)) + " (" + durationDays + "d)<br>" +
                                "Status: " + t.getStatus().getLabel() + " | Priority: " + t.getPriority().getLabel() + "<br>" +
                                "Logged: " + String.format("%.1f", t.getLoggedHours()) + "h / " + String.format("%.1f", t.getEstimatedHours()) + "h<br>" +
                                "<small class='text-warning-emphasis'>&#x2728; Click to open files &amp; attachments</small>";
                %>
                <div class="gantt-chart-row" data-task-id="<%= t.getId() %>" data-status="<%= t.getStatus().name() %>" data-priority="<%= t.getPriority().name() %>" data-title="<%= titleEsc.toLowerCase() %>" data-assignee="<%= assignee.toLowerCase() %>">
                    <div class="gantt-bar-item <%= barClass %>"
                         style="left: <%= String.format(java.util.Locale.US, "%.2f", leftPercent) %>%; width: <%= String.format(java.util.Locale.US, "%.2f", widthPercent) %>%;"
                         data-bs-toggle="tooltip" data-bs-html="true" data-bs-title="<%= ValidationUtil.escapeXml(tooltipHtml) %>"
                         onclick="openTaskAttachmentsModal(<%= t.getId() %>, '<%= titleEsc.replace("'", "\\'") %>')">
                        
                        <!-- Internal Progress Bar Fill -->
                        <div class="gantt-bar-progress" style="width: <%= progressFill %>%;"></div>

                        <!-- Content inside Bar -->
                        <div class="gantt-bar-content">
                            <% if (t.isOverdue()) { %>
                            <i class="bi bi-alarm text-white"></i>
                            <% } else if (t.getStatus() == TaskStatus.COMPLETED) { %>
                            <i class="bi bi-check2-circle text-white"></i>
                            <% } %>
                            <span class="text-truncate"><%= titleEsc %></span>
                            <span class="badge bg-black bg-opacity-25 py-0 px-1 text-white ms-auto" style="font-size: 0.65rem;">
                                <%= durationDays %>d
                            </span>
                        </div>
                    </div>
                </div>
                <%   }
                   } else { %>
                <div class="py-5 text-center text-muted">
                    <i class="bi bi-bar-chart-steps fs-1 d-block mb-2 text-secondary opacity-50"></i>
                    No tasks currently scheduled to display in the Gantt timeline.
                </div>
                <% } %>
            </div>
        </div>
    </div>

    <!-- 3. Legend Footer -->
    <div class="gantt-legend">
        <span class="fw-bold text-dark"><i class="bi bi-info-circle me-1"></i> Legend:</span>
        <div class="gantt-legend-item">
            <span class="gantt-legend-color gantt-bar-todo"></span>
            <span>To Do</span>
        </div>
        <div class="gantt-legend-item">
            <span class="gantt-legend-color gantt-bar-inprogress"></span>
            <span>In Progress</span>
        </div>
        <div class="gantt-legend-item">
            <span class="gantt-legend-color gantt-bar-review"></span>
            <span>In Review</span>
        </div>
        <div class="gantt-legend-item">
            <span class="gantt-legend-color gantt-bar-completed"></span>
            <span>Completed</span>
        </div>
        <div class="gantt-legend-item">
            <span class="gantt-legend-color gantt-bar-overdue"></span>
            <span>Overdue Task</span>
        </div>
        <div class="gantt-legend-item">
            <span style="width: 14px; height: 3px; background-color: #ef4444; display: inline-block;"></span>
            <span>Today's Date Marker</span>
        </div>
    </div>
</div>

<!-- Gantt Chart Client Interactivity Script -->
<script>
document.addEventListener('DOMContentLoaded', function () {
    // Initialize tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('#ganttBoardContainer [data-bs-toggle="tooltip"]'));
    tooltipTriggerList.forEach(function (tooltipTriggerEl) {
        new bootstrap.Tooltip(tooltipTriggerEl, { container: 'body' });
    });
});

let currentGanttStatusFilter = 'ALL';
let currentGanttPriorityFilter = 'ALL';
let currentGanttSearchQuery = '';

function applyGanttFilters() {
    const sidebarRows = document.querySelectorAll('#ganttSidebarList .gantt-sidebar-row');
    const chartRows = document.querySelectorAll('#ganttChartRowsContainer .gantt-chart-row');

    sidebarRows.forEach((sRow, idx) => {
        const cRow = chartRows[idx];
        const status = sRow.getAttribute('data-status');
        const priority = sRow.getAttribute('data-priority');
        const title = sRow.getAttribute('data-title') || '';
        const assignee = sRow.getAttribute('data-assignee') || '';

        const matchStatus = (currentGanttStatusFilter === 'ALL' || status === currentGanttStatusFilter);
        const matchPriority = (currentGanttPriorityFilter === 'ALL' || priority === currentGanttPriorityFilter);
        const matchSearch = (!currentGanttSearchQuery || title.includes(currentGanttSearchQuery) || assignee.includes(currentGanttSearchQuery));

        const visible = matchStatus && matchPriority && matchSearch;
        sRow.style.display = visible ? '' : 'none';
        if (cRow) cRow.style.display = visible ? '' : 'none';
    });
}

window.filterGanttStatus = function(status, btn) {
    currentGanttStatusFilter = status;
    const group = btn.closest('.btn-group');
    if (group) {
        group.querySelectorAll('.btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
    }
    applyGanttFilters();
};

window.filterGanttPriority = function(priority) {
    currentGanttPriorityFilter = priority;
    applyGanttFilters();
};

window.filterGanttSearch = function(query) {
    currentGanttSearchQuery = (query || '').toLowerCase().trim();
    applyGanttFilters();
};

window.toggleGanttDensity = function(compact) {
    const btnNormal = document.getElementById('btnGanttNormal');
    const btnCompact = document.getElementById('btnGanttCompact');
    if (btnNormal && btnCompact) {
        btnNormal.classList.toggle('active', !compact);
        btnCompact.classList.toggle('active', compact);
    }

    document.querySelectorAll('.gantt-sidebar-row').forEach(row => {
        row.classList.toggle('compact-mode', compact);
    });
    document.querySelectorAll('.gantt-chart-row').forEach(row => {
        row.classList.toggle('compact-mode', compact);
    });
};
</script>
