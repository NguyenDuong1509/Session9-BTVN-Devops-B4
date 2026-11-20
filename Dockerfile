# ==============================================================================
# Dockerfile - Quản lý nhân sự (Employee Service)
# Session 9 - Bài tập 4: Đóng gói Docker Image từ Artifact JAR
# ==============================================================================

# Stage Runtime: Sử dụng JRE 17 Alpine siêu nhẹ (~139MB)
FROM eclipse-temurin:17-jre-alpine

# Thiết lập thư mục làm việc trong container
WORKDIR /app

# Sao chép file artifact JAR được truyền từ stage build trong CI/CD Pipeline
COPY build/libs/employee-service-1.0.0.jar app.jar

# Khai báo cổng lắng nghe của Microservice
EXPOSE 8080

# Lệnh khởi chạy ứng dụng Spring Boot dạng Exec Form (PID 1)
ENTRYPOINT ["java", "-jar", "app.jar"]
