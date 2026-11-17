<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List" %>
<%@ page import="com.protrack.model.User" %>
<%@ page import="com.protrack.model.ActivityLog" %>
<%@ page import="com.protrack.util.DateUtil" %>
<%@ page import="com.protrack.util.ValidationUtil" %>
<%
    User profileUser = (User) session.getAttribute("currentUser");
    request.setAttribute("pageTitle", "User Profile");
    request.setAttribute("pageSubtitle", "Manage personal account credentials and review account security");
    request.setAttribute("activeMenu", "profile");

    @SuppressWarnings("unchecked")
    List<ActivityLog> myActivities = (List<ActivityLog>) request.getAttribute("myActivities");
%>

<jsp:include page="common/header.jsp" />
<jsp:include page="common/sidebar.jsp" />

<div class="main-wrapper">
    <jsp:include page="common/navbar.jsp" />

    <div class="content-body">
        <jsp:include page="common/alerts.jsp" />

        <div class="row g-4">
            <!-- Left Column: User Profile Card & Information Form -->
            <div class="col-lg-6">
                <!-- Profile Overview Header -->
                <div class="protrack-card mb-4">
                    <div class="protrack-card-body p-4 text-center">
                        <div class="position-relative d-inline-block mb-3">
                            <% if (profileUser != null && profileUser.hasProfilePicture()) { %>
                                <img src="<%= ValidationUtil.escapeXml(profileUser.getProfilePicture()) %>"
                                     alt="Profile Picture" id="currentAvatarImg"
                                     class="rounded-circle object-fit-cover shadow-sm border"
                                     style="width: 84px; height: 84px; border: 3px solid #6366f1 !important;">
                            <% } else { %>
                                <div class="rounded-circle bg-primary text-white fw-bold d-inline-flex align-items-center justify-content-center"
                                     id="currentAvatarInitial"
                                     style="width: 84px; height: 84px; font-size: 2rem; box-shadow: 0 4px 10px rgba(99, 102, 241, 0.3);">
                                    <%= profileUser != null && !profileUser.getFullName().isEmpty() ? ValidationUtil.escapeXml(profileUser.getFullName().substring(0, 1).toUpperCase()) : "U" %>
                                </div>
                            <% } %>
                            <button type="button" class="btn btn-sm btn-primary rounded-circle position-absolute bottom-0 end-0 p-0 shadow d-flex align-items-center justify-content-center"
                                    style="width: 28px; height: 28px; font-size: 0.75rem;"
                                    title="Change Profile Picture" data-bs-toggle="modal" data-bs-target="#avatarModal">
                                <i class="bi bi-camera-fill"></i>
                            </button>
                        </div>
                        <div class="mb-2">
                            <button type="button" class="btn btn-sm btn-outline-custom" data-bs-toggle="modal" data-bs-target="#avatarModal">
                                <i class="bi bi-image me-1"></i> Change Photo
                            </button>
                        </div>
                        <h4 class="fw-bold text-dark mb-1"><%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getFullName()) : "" %></h4>
                        <div class="text-secondary small mb-2"><%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getEmail()) : "" %></div>
                        <span class="badge badge-custom badge-role-<%= profileUser != null ? profileUser.getRole().name() : "" %>">
                            <i class="bi bi-shield-check"></i> <%= profileUser != null ? profileUser.getRole().getDisplayName() : "" %>
                        </span>
                        <div class="text-muted small mt-3">
                            Account ID: #<%= profileUser != null ? profileUser.getId() : "" %> &bull; Status:
                            <span class="text-success fw-medium">Active</span>
                        </div>
                    </div>
                </div>

                <!-- Update Name & Email Form -->
                <div class="protrack-card">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-person-gear"></i> Personal Details
                        </h5>
                    </div>
                    <div class="protrack-card-body p-4">
                        <form action="<%= request.getContextPath() %>/profile" method="POST">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="updateProfile">
                            <div class="mb-3">
                                <label for="profName" class="form-label">Full Name</label>
                                <input type="text" class="form-control" id="profName" name="fullName"
                                       value="<%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getFullName()) : "" %>" required>
                            </div>
                            <div class="mb-3">
                                <label for="profEmail" class="form-label">Email Address</label>
                                <input type="email" class="form-control" id="profEmail" name="email"
                                       value="<%= profileUser != null ? ValidationUtil.escapeXml(profileUser.getEmail()) : "" %>" required>
                            </div>
                            <div class="d-flex justify-content-end">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save me-1"></i> Update Details
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>

            <!-- Right Column: Security (Change Password) & Recent History -->
            <div class="col-lg-6">
                <!-- Change Password Form -->
                <div class="protrack-card mb-4">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-shield-lock"></i> Security & Password
                        </h5>
                    </div>
                    <div class="protrack-card-body p-4">
                        <form action="<%= request.getContextPath() %>/profile" method="POST">
                            <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                            <input type="hidden" name="action" value="changePassword">
                            <div class="mb-3">
                                <label for="currPass" class="form-label">Current Password</label>
                                <input type="password" class="form-control" id="currPass" name="currentPassword" required>
                            </div>
                            <div class="mb-3">
                                <label for="newPass" class="form-label">New Password</label>
                                <input type="password" class="form-control" id="newPass" name="newPassword" minlength="8" placeholder="Min 8 characters" required>
                            </div>
                            <div class="mb-4">
                                <label for="confPass" class="form-label">Confirm New Password</label>
                                <input type="password" class="form-control" id="confPass" name="confirmPassword" minlength="8" required>
                            </div>
                            <div class="d-flex justify-content-end">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-key me-1"></i> Update Password
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <!-- Recent Activity Timeline -->
                <div class="protrack-card">
                    <div class="protrack-card-header">
                        <h5 class="protrack-card-title">
                            <i class="bi bi-clock-history"></i> My Recent Audit Trail
                        </h5>
                    </div>
                    <div class="protrack-card-body">
                        <% if (myActivities != null && !myActivities.isEmpty()) { %>
                        <div class="timeline">
                            <% for (ActivityLog act : myActivities) { %>
                            <div class="timeline-item">
                                <div class="timeline-marker"></div>
                                <div class="timeline-content">
                                    <div class="timeline-title"><%= ValidationUtil.escapeXml(act.getDetails()) %></div>
                                    <div class="timeline-time">
                                        <i class="bi bi-clock me-1"></i><%= act.getFormattedCreatedAt() %>
                                    </div>
                                </div>
                            </div>
                            <% } %>
                        </div>
                        <% } else { %>
                        <p class="text-muted small text-center my-3">No activity logs recorded yet.</p>
                        <% } %>
                    </div>
                </div>
            </div>
        </div>

    </div><!-- /.content-body -->

<!-- Modal: Update Profile Picture -->
<div class="modal fade" id="avatarModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 shadow">
            <div class="modal-header bg-light">
                <h6 class="modal-title fw-bold"><i class="bi bi-camera text-primary me-2"></i>Update Profile Picture</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body p-4 text-center">
                <!-- Image Preview -->
                <div class="mb-3">
                    <img id="avatarPreview" src="<%= profileUser != null && profileUser.hasProfilePicture() ? ValidationUtil.escapeXml(profileUser.getProfilePicture()) : "https://ui-avatars.com/api/?name=" + (profileUser != null ? profileUser.getFullName() : "User") + "&background=6366f1&color=fff&size=128" %>" 
                         alt="Preview" class="rounded-circle object-fit-cover shadow-sm border" style="width: 110px; height: 110px; border: 3px solid #e0e7ff !important;">
                </div>

                <form id="avatarForm" action="<%= request.getContextPath() %>/profile" method="POST">
                    <input type="hidden" name="csrfToken" value="${sessionScope.csrfToken}">
                    <input type="hidden" id="avatarAction" name="action" value="updateProfilePicture">
                    <input type="hidden" id="profilePictureData" name="profilePicture" value="<%= profileUser != null && profileUser.hasProfilePicture() ? ValidationUtil.escapeXml(profileUser.getProfilePicture()) : "" %>">

                    <!-- File Upload Option -->
                    <div class="mb-3 text-start">
                        <label class="form-label small fw-semibold">Upload Image from Computer</label>
                        <input type="file" id="avatarFileInput" class="form-control form-control-sm" accept="image/png, image/jpeg, image/jpg, image/webp" onchange="previewAvatarFile(this)">
                        <div class="form-text" style="font-size: 0.75rem;">Supported formats: JPG, PNG, WEBP (Max 2MB)</div>
                    </div>

                    <!-- URL Option -->
                    <div class="mb-3 text-start">
                        <label class="form-label small fw-semibold">Or Image URL</label>
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-light text-muted"><i class="bi bi-link-45deg"></i></span>
                            <input type="url" id="avatarUrlInput" class="form-control" placeholder="https://example.com/avatar.jpg" oninput="previewAvatarUrl(this.value)">
                        </div>
                    </div>

                    <div class="d-flex justify-content-between align-items-center pt-2 border-top">
                        <% if (profileUser != null && profileUser.hasProfilePicture()) { %>
                        <button type="button" class="btn btn-sm btn-outline-danger" onclick="submitRemoveAvatar()">
                            <i class="bi bi-trash3 me-1"></i> Remove Photo
                        </button>
                        <% } else { %>
                        <div></div>
                        <% } %>
                        <div class="d-flex gap-2">
                            <button type="button" class="btn btn-sm btn-outline-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-sm btn-primary">
                                <i class="bi bi-check-lg me-1"></i> Save Picture
                            </button>
                        </div>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

<script>
    function previewAvatarFile(input) {
        if (input.files && input.files[0]) {
            const file = input.files[0];
            if (file.size > 2 * 1024 * 1024) {
                alert('Image size exceeds 2MB limit. Please choose a smaller image.');
                input.value = '';
                return;
            }
            const reader = new FileReader();
            reader.onload = function(e) {
                const dataUrl = e.target.result;
                document.getElementById('avatarPreview').src = dataUrl;
                document.getElementById('profilePictureData').value = dataUrl;
            };
            reader.readAsDataURL(file);
        }
    }

    function previewAvatarUrl(url) {
        url = url.trim();
        if (url) {
            document.getElementById('avatarPreview').src = url;
            document.getElementById('profilePictureData').value = url;
        }
    }

    function submitRemoveAvatar() {
        if (confirm('Are you sure you want to remove your profile picture?')) {
            document.getElementById('avatarAction').value = 'removeProfilePicture';
            document.getElementById('profilePictureData').value = '';
            document.getElementById('avatarForm').submit();
        }
    }
</script>

<jsp:include page="common/footer.jsp" />
