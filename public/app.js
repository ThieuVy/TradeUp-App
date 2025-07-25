document.addEventListener("DOMContentLoaded", () => {
    const db = firebase.firestore();
    const auth = firebase.auth();
    const functions = firebase.app().functions('us-central1');

    const loginContainer = document.getElementById("login-container");
    const dashboardContainer = document.getElementById("dashboard-container");
    const loginButton = document.getElementById("login-button");
    const logoutButton = document.getElementById("logout-button");
    const adminEmailInput = document.getElementById("admin-email");
    const adminPasswordInput = document.getElementById("admin-password");
    const loginError = document.getElementById("login-error");
    const adminUserEmail = document.getElementById("admin-user-email");

    const pendingReviewsList = document.getElementById("pending-reviews-list");
    const reviewCountSpan = document.getElementById("review-count");

    const pendingReportsList = document.getElementById("pending-reports-list");
    const reportCountSpan = document.getElementById("report-count");

    auth.onAuthStateChanged(async (user) => {
        if (user) {
            try {
                const idTokenResult = await user.getIdTokenResult(true);
                if (idTokenResult.claims.admin) {
                    console.log("Xác thực thành công, người dùng là Admin.");
                    showDashboard(user);
                } else {
                    console.log("Người dùng không có quyền Admin. Đang đăng xuất...");
                    await auth.signOut();
                    showLogin("Bạn không có quyền truy cập vào trang quản trị.");
                }
            } catch (error) {
                console.error("Lỗi khi kiểm tra quyền admin:", error);
                await auth.signOut();
                showLogin("Đã xảy ra lỗi trong quá trình xác thực. Vui lòng thử lại.");
            }
        } else {
            showLogin();
        }
    });

    loginButton.addEventListener("click", () => {
        const email = adminEmailInput.value;
        const password = adminPasswordInput.value;
        if (!email || !password) {
            loginError.textContent = "Vui lòng nhập đầy đủ email và mật khẩu.";
            return;
        }
        loginError.textContent = "";
        auth.signInWithEmailAndPassword(email, password).catch(error => {
            console.error("Lỗi đăng nhập:", error.code, error.message);
            loginError.textContent = "Email hoặc mật khẩu không chính xác.";
        });
    });

    logoutButton.addEventListener("click", () => auth.signOut());

    function showDashboard(user) {
        loginContainer.classList.add("hidden");
        dashboardContainer.classList.remove("hidden");
        adminUserEmail.textContent = user.email;
        listenForPendingReviews();
        listenForPendingReports();
    }

    function showLogin(errorMessage = "") {
        loginContainer.classList.remove("hidden");
        dashboardContainer.classList.add("hidden");
        loginError.textContent = errorMessage;
    }

    function listenForPendingReviews() {
        db.collection("reviews").where("moderationStatus", "==", "pending").onSnapshot(snapshot => {
            pendingReviewsList.innerHTML = "";
            reviewCountSpan.textContent = snapshot.size;
            if (snapshot.empty) {
                pendingReviewsList.innerHTML = "<p>Không có đánh giá nào cần duyệt.</p>";
                return;
            }
            snapshot.forEach(doc => {
                const reviewCard = createReviewCard(doc.id, doc.data());
                pendingReviewsList.appendChild(reviewCard);
            });
        }, err => console.error("Lỗi lắng nghe reviews:", err));
    }

    function createReviewCard(id, review) {
        const card = document.createElement("div");
        card.className = "item-card";
        card.innerHTML = `
            <p><strong>Người đánh giá:</strong> ${review.reviewerName || "N/A"} (${review.reviewerId})</p>
            <p><strong>Người được đánh giá:</strong> ${review.reviewedUserId}</p>
            <p><strong>Đánh giá:</strong> ${"⭐".repeat(review.rating)}</p>
            <p class="comment"><strong>Nội dung:</strong> "${review.comment || "Không có bình luận."}"</p>
            <div class="actions">
                <button class="approve-btn" data-id="${id}">Duyệt</button>
                <button class="reject-btn" data-id="${id}">Từ chối</button>
            </div>`;
        card.querySelector(".approve-btn").addEventListener("click", () => moderateReview(id, "approved"));
        card.querySelector(".reject-btn").addEventListener("click", () => moderateReview(id, "rejected"));
        return card;
    }

    function moderateReview(reviewId, newStatus) {
        const func = functions.httpsCallable('moderateReview');
        func({ reviewId, newStatus })
            .then(res => {
                Swal.fire('Thành công!', res.data.message, 'success');
            })
            .catch(err => {
                Swal.fire('Lỗi!', err.message, 'error');
            });
    }

    function listenForPendingReports() {
        db.collection("reports").where("status", "==", "pending").orderBy("timestamp", "desc").onSnapshot(snapshot => {
            pendingReportsList.innerHTML = "";
            reportCountSpan.textContent = snapshot.size;
            if (snapshot.empty) {
                pendingReportsList.innerHTML = "<p>Không có báo cáo nào cần xử lý.</p>";
                return;
            }
            snapshot.forEach(doc => {
                const reportCard = createReportCard(doc.id, doc.data());
                pendingReportsList.appendChild(reportCard);
            });
        }, err => console.error("Lỗi lắng nghe reports:", err));
    }

    function createReportCard(id, report) {
        const card = document.createElement("div");
        card.className = "item-card";
        const reportTime = report.timestamp ? new Date(report.timestamp.seconds * 1000).toLocaleString('vi-VN') : 'N/A';
        card.innerHTML = `
            <p><strong>Loại báo cáo:</strong> ${report.type || 'N/A'}</p>
            <p><strong>Người báo cáo:</strong> ${report.reporterId}</p>
            <p><strong>Đối tượng bị báo cáo:</strong> ${report.reportedUserId || report.reportedListingId}</p>
            <p><strong>Lý do:</strong> ${report.reason || "Không có lý do."}</p>
            <p><strong>Thời gian:</strong> ${reportTime}</p>
            <div class="actions">
                <button class="reject-btn" data-report-id="${id}" data-reported-uid="${report.reportedUserId}">Xử lý & Treo tài khoản</button>
                <button class="approve-btn" data-report-id="${id}" data-reported-uid="${report.reportedUserId}">Xử lý (Bỏ qua)</button>
            </div>
        `;
        card.querySelector(".reject-btn").addEventListener("click", (e) => resolveReport(e.target.dataset.reportId, e.target.dataset.reportedUid, true));
        card.querySelector(".approve-btn").addEventListener("click", (e) => resolveReport(e.target.dataset.reportId, e.target.dataset.reportedUid, false));
        return card;
    }

    function resolveReport(reportId, reportedUserId, shouldSuspend) {
        const confirmationMessage = `Bạn có chắc muốn giải quyết báo cáo này? ${shouldSuspend ? 'Hành động này sẽ TREO tài khoản người dùng.' : ''}`;

        // Thay vì dùng confirm()
        Swal.fire({
            title: 'Xác nhận hành động',
            text: confirmationMessage,
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3085d6',
            cancelButtonColor: '#d33',
            confirmButtonText: 'Đồng ý!',
            cancelButtonText: 'Hủy'
        }).then((result) => {
            if (result.isConfirmed) {
                const func = functions.httpsCallable('resolveReport');
                func({ reportId, reportedUserId, shouldSuspend })
                    .then(res => {
                        console.log(res.data.message);
                        // Thay vì alert()
                        Swal.fire('Thành công!', res.data.message, 'success');
                    })
                    .catch(err => {
                        Swal.fire('Lỗi!', err.message, 'error');
                    });
            }
        });
    }
    const recalculateButton = document.getElementById("recalculate-stats-button");
        if (recalculateButton) {
            recalculateButton.addEventListener("click", () => {
                Swal.fire({
                    title: 'Bạn có chắc không?',
                    text: "Hành động này sẽ tính toán lại số tin đăng và giao dịch cho TẤT CẢ người dùng. Việc này có thể mất một lúc.",
                    icon: 'warning',
                    showCancelButton: true,
                    confirmButtonColor: '#3085d6',
                    cancelButtonColor: '#d33',
                    confirmButtonText: 'Đồng ý, chạy đi!',
                    cancelButtonText: 'Hủy'
                }).then((result) => {
                    if (result.isConfirmed) {
                        recalculateButton.disabled = true;
                        recalculateButton.textContent = "Đang xử lý...";

                        const func = functions.httpsCallable('recalculateAllUserStats');
                        func({})
                            .then(res => {
                                Swal.fire('Thành công!', res.data.message, 'success');
                                recalculateButton.disabled = false;
                                recalculateButton.textContent = "Đồng bộ lại Số liệu Thống kê Người dùng";
                            })
                            .catch(err => {
                                Swal.fire('Lỗi!', err.message, 'error');
                                recalculateButton.disabled = false;
                                recalculateButton.textContent = "Đồng bộ lại Số liệu Thống kê Người dùng";
                            });
                    }
                });
            });
        }
});