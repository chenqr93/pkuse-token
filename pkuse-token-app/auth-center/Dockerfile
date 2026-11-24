# 鉴权中心（SSO Client + 权限中心，9010）——最小化运行镜像
# 构建（需先 mvn -DskipTests package -P dev）：docker build -t docker.fz.com/xzzf/auth-center:<tag> .
FROM docker.fz.com/base/jre17-min:17.0.19

LABEL org.opencontainers.image.title="auth-center" \
      org.opencontainers.image.description="执法产品鉴权中心（SSO Client + 菜单权限）" \
      org.opencontainers.image.licenses="Proprietary"

ENV TZ=Asia/Shanghai

WORKDIR /app
COPY authcenter-web/target/authcenter-exec.jar app.jar

EXPOSE 9010

ENTRYPOINT ["java", "-jar", "app.jar"]
