# QuickBite Microservices - Employee Service (Docker Hub Release)

> **Session 9 - Bài tập 4:** Đẩy Docker Image lên Docker Hub thông qua GitLab CI (Vận dụng chuyên sâu)

## 📌 Tổng quan dự án
Dự án hoàn thiện pipeline CI/CD 3 giai đoạn tự động hóa từ khâu kiểm thử với PostgreSQL, đóng gói ứng dụng Spring Boot thành file JAR, đến khâu đóng gói Docker Image qua kỹ thuật Docker-in-Docker (DinD) và phát hành trực tiếp lên Docker Hub Registry.

---

## 🚀 Cấu trúc Pipeline 3 Stages (`.gitlab-ci.yml`)

1. **Stage 1 (`test`):**
   - Khởi chạy sidecar container `postgres:14-alpine`.
   - Chạy toàn bộ Unit & Integration Tests với `./gradlew test`.
2. **Stage 2 (`build`):**
   - Chỉ chạy khi Stage 1 PASS.
   - Biên dịch và đóng gói JAR: `./gradlew build -x test`.
   - Lưu trữ kết quả `build/libs/*.jar` dạng `artifacts` trong 1 ngày.
3. **Stage 3 (`dockerize`):**
   - Sử dụng `docker:latest` và `docker:dind`.
   - Nhận file JAR từ artifacts của `build_job`.
   - Đăng nhập bảo mật Docker Hub qua GitLab CI/CD Variables (`$DOCKER_USERNAME`, `$DOCKER_PASSWORD`).
   - Build và push image `nguyenduong1509/employee-service:1.0.0` và `:latest` lên Docker Hub.

---

## 🔐 Thiết lập GitLab CI/CD Variables
Vào **Settings > CI/CD > Variables**:
- `DOCKER_USERNAME`: `nguyenduong1509`
- `DOCKER_PASSWORD`: Personal Access Token Docker Hub (bật **Masked**)

---

## 📸 Minh chứng hoàn thành
- [Báo cáo kỹ thuật chi tiết](file:///EXPLANATION.md)
- [Ảnh cài đặt CI/CD Variables trên GitLab](file:///homework/session_09/exercise_04/gitlab_variables_settings.png)
- [Ảnh Docker Hub Repository](file:///homework/session_09/exercise_04/docker_hub_repository_proof.png)
- [Ảnh Pipeline 3 Stages thực thi thành công](file:///homework/session_09/exercise_04/gitlab_ci_dockerize_pipeline_proof.png)
