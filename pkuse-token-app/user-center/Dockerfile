# 用户中心（SSO Server，9000）——最小化运行镜像
# 构建（需先 mvn -DskipTests package -P dev）：docker build -t docker.fz.com/xzzf/user-center:<tag> .
FROM docker.fz.com/base/jre17-min:17.0.19

LABEL org.opencontainers.image.title="user-center" \
      org.opencontainers.image.description="执法产品用户中心（SSO Server）" \
      org.opencontainers.image.licenses="Proprietary"

ENV TZ=Asia/Shanghai

WORKDIR /app
COPY usercenter-web/target/usercenter-exec.jar app.jar

EXPOSE 9000

# 配置覆盖：SPRING_CONFIG_ADDITIONAL_LOCATION 指向挂载的 /app/config/（部署外置配置）
# JVM 内存：基础镜像 JAVA_TOOL_OPTIONS 已含 -XX:MaxRAMPercentage=75.0
ENTRYPOINT ["java", "-jar", "app.jar"]
