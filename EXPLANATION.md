# BÁO CÁO KỸ THUẬT: ĐẨY DOCKER IMAGE LÊN DOCKER HUB THÔNG QUA GITLAB CI (SESSION 9 - BÀI 4)

## 1. Bối cảnh & Yêu cầu nghiệp vụ
- **Dự án:** Quản lý nhân sự (`employee-service` / QuickBite HR Platform).
- **Mục tiêu:** Tự động hóa hoàn toàn quy trình từ kiểm thử CSDL, đóng gói file JAR đến việc build Docker image và phát hành (push) lên Docker Hub Registry.
- **Nguyên tắc an toàn bảo mật:** Tuyệt đối **không hard-code** tên đăng nhập (`username`) và mật khẩu/token (`password`) vào file `.gitlab-ci.yml` hoặc mã nguồn. Toàn bộ thông tin nhạy cảm phải được quản lý thông qua cơ chế **GitLab CI/CD Variables** và bật cờ **Masked**.

---

## 2. Toàn văn cấu hình `.gitlab-ci.yml` chuẩn xác

```yaml
# ==============================================================================
# GitLab CI/CD Pipeline - Quản lý nhân sự (Employee Service)
# Bài tập 4: Đẩy Docker Image lên Docker Hub thông qua GitLab CI (Vận dụng chuyên sâu)
# ==============================================================================

# Định nghĩa 3 stages tuần tự trong quy trình CI/CD
stages:
  - test
  - build
  - dockerize

# Cấu hình biến môi trường toàn cục
variables:
  # Cấu hình Database PostgreSQL cho Stage Test
  POSTGRES_DB: hr_db
  POSTGRES_USER: hr_user
  POSTGRES_PASSWORD: hr_password
  POSTGRES_HOST_AUTH_METHOD: trust
  # Cấu hình Docker Image Tag & DinD TLS
  IMAGE_TAG: "1.0.0"
  DOCKER_TLS_CERTDIR: ""

# ------------------------------------------------------------------------------
# Stage 1: Kiểm thử tích hợp tự động với PostgreSQL Database (Test Stage)
# ------------------------------------------------------------------------------
test_job:
  stage: test
  image: gradle:7.6-jdk17
  # Khai báo service PostgreSQL chạy song song với container kiểm thử
  services:
    - postgres:14-alpine
  variables:
    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/hr_db
    SPRING_DATASOURCE_USERNAME: hr_user
    SPRING_DATASOURCE_PASSWORD: hr_password
    SPRING_DATASOURCE_DRIVER: org.postgresql.Driver
  before_script:
    - chmod +x ./gradlew
  script:
    - echo "================================================================"
    - echo "===> Stage 1: Chạy Unit & Integration Tests với PostgreSQL..."
    - echo "================================================================"
    - ./gradlew test

# ------------------------------------------------------------------------------
# Stage 2: Đóng gói ứng dụng thành file JAR & Lưu Artifacts (Build Stage)
# ------------------------------------------------------------------------------
build_job:
  stage: build
  image: gradle:7.6-jdk17
  before_script:
    - chmod +x ./gradlew
  script:
    - echo "================================================================"
    - echo "===> Stage 2: Đóng gói ứng dụng thành file JAR..."
    - echo "================================================================"
    - ./gradlew build -x test
  # Cấu hình lưu trữ file JAR sinh ra dưới dạng artifacts
  artifacts:
    name: "employee-service-build-$CI_COMMIT_SHORT_SHA"
    paths:
      - build/libs/*.jar
    expire_in: 1 day

# ------------------------------------------------------------------------------
# Stage 3: Đóng gói Docker Image & Push lên Docker Hub (Dockerize Stage)
# ------------------------------------------------------------------------------
dockerize_job:
  stage: dockerize
  image: docker:latest
  # Khởi chạy Docker daemon bên trong container (Docker-in-Docker)
  services:
    - docker:dind
  # Kế thừa artifacts chứa file JAR từ build_job
  dependencies:
    - build_job
  before_script:
    - echo "================================================================"
    - echo "===> Đăng nhập Docker Hub sử dụng biến bí mật (CI/CD Variables)..."
    - echo "================================================================"
    - echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
  script:
    - echo "================================================================"
    - echo "===> Đóng gói Docker Image từ file JAR trong artifacts..."
    - echo "================================================================"
    - docker build -t "$DOCKER_USERNAME/employee-service:$IMAGE_TAG" -t "$DOCKER_USERNAME/employee-service:latest" .
    - echo "================================================================"
    - echo "===> Đẩy Docker Image lên Docker Hub Repository..."
    - echo "================================================================"
    - docker push "$DOCKER_USERNAME/employee-service:$IMAGE_TAG"
    - docker push "$DOCKER_USERNAME/employee-service:latest"
```

---

## 3. Hướng dẫn thiết lập CI/CD Variables trên GitLab (Bảo mật thông tin)

Để pipeline có thể đăng nhập vào Docker Hub mà không để lộ mật khẩu trong code, thực hiện theo các bước sau:

1. Truy cập vào dự án trên **GitLab**.
2. Trên menu bên trái, chọn **Settings** $\rightarrow$ **CI/CD**.
3. Cuộn đến mục **Variables**, bấm **Expand** $\rightarrow$ Chọn **Add variable**.
4. Khởi tạo 2 biến sau:
   - **Biến 1: `DOCKER_USERNAME`**
     - **Key:** `DOCKER_USERNAME`
     - **Value:** `nguyenduong1509` (Tài khoản Docker Hub của bạn)
     - **Type:** Variable
     - **Flags:** Bỏ tick Protect variable (hoặc giữ nếu branch là protected), tick **Expand variable reference**.
   - **Biến 2: `DOCKER_PASSWORD`**
     - **Key:** `DOCKER_PASSWORD`
     - **Value:** `<Personal_Access_Token_hoặc_Password_DockerHub>`
     - **Type:** Variable
     - **Flags:** **Tick chọn "Mask variable"** (Bắt buộc để GitLab tự động che giấu giá trị `[MASKED]` trên console log nếu vô tình in ra).

---

## 4. Phân tích chi tiết các kỹ thuật trong Stage `dockerize`

### 4.1. Kiến trúc Docker-in-Docker (DinD)
- **Image & Service:** `image: docker:latest` đi kèm `services: - docker:dind`.
- **Cơ chế:** Container chính chứa Docker CLI, trong khi service container chạy một Docker daemon riêng biệt độc lập. Docker CLI gửi các lệnh (`build`, `login`, `push`) tới daemon này thông qua socket mạng.
- **Biến `DOCKER_TLS_CERTDIR: ""`:** Tắt yêu cầu chứng chỉ TLS nội bộ giữa Docker CLI và Docker daemon DinD, giúp kết nối đơn giản, nhanh chóng và ổn định trong môi trường runner mặc định.

### 4.2. Bảo mật khi đăng nhập Registry (`docker login`)
- Thay vì dùng `docker login -u user -p pass` (dễ bị lộ trong process table của hệ điều hành), chúng ta sử dụng chuẩn:
  ```bash
  echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin
  ```
  Lệnh đọc mật khẩu trực tiếp từ luồng stdin, tuyệt đối an toàn và không lưu vết trong lịch sử terminal.

### 4.3. Kế thừa Artifacts từ Stage `build` (`dependencies: - build_job`)
- Nhờ vào cơ chế quản lý artifacts của GitLab CI, `dockerize_job` tự động tải về thư mục `build/libs/*.jar` đã được biên dịch thành công từ `build_job`.
- File `Dockerfile` đặt tại thư mục gốc chỉ việc thực thi:
  ```dockerfile
  COPY build/libs/employee-service-1.0.0.jar app.jar
  ```
  Giúp quá trình build Docker image cực kỳ nhanh (dưới 10 giây) vì không cần phải build lại mã nguồn Java!

### 4.4. Quy tắc gắn thẻ đa phiên bản (Dual Tagging)
- Hệ thống tự động gán đồng thời 2 thẻ:
  - Thẻ phiên bản cụ thể: `nguyenduong1509/employee-service:1.0.0` (phục vụ truy vết release).
  - Thẻ phiên bản mới nhất: `nguyenduong1509/employee-service:latest` (phục vụ môi trường dev/staging tự động pull).

---

## 5. Hướng dẫn nộp bài lên Portal

1. **File cấu hình:** Nộp file `.gitlab-ci.yml`.
2. **Đường dẫn Docker Hub Repository:**
   ```text
   https://hub.docker.com/r/nguyenduong1509/employee-service
   ```
3. **Ảnh minh chứng đã chuẩn bị:**
   - `homework/session_09/exercise_04/gitlab_variables_settings.png`: Ảnh chụp giao diện cài đặt CI/CD Variables trên GitLab (`DOCKER_USERNAME`, `DOCKER_PASSWORD`).
   - `homework/session_09/exercise_04/docker_hub_repository_proof.png`: Ảnh chụp giao diện Repository `nguyenduong1509/employee-service` trên Docker Hub với các tags `1.0.0` và `latest`.
   - `homework/session_09/exercise_04/gitlab_ci_dockerize_pipeline_proof.png`: Ảnh chụp log pipeline 3 stages chạy thành công rực rỡ.
4. **Link GitHub repository nộp bài:**
   ```text
   https://github.com/NguyenDuong1509/Session9-BTVN-Devops-B4
   ```
