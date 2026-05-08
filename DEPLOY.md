# 轻量应用服务器部署说明

适用环境:

- 服务器镜像: `Ubuntu 22.04`
- 容器环境: `Docker 26`
- 项目结构: `MySQL 8 + Spring Boot + Nginx`

## 1. 项目当前部署方式

仓库根目录已经提供了这些文件:

- `docker-compose.yml`
- `deploy/init.sql`
- `lottery-backend/Dockerfile`
- `lottery-frontend/Dockerfile`
- `lottery-frontend/nginx.conf`

这套配置的职责是:

- `mysql`: 启动 MySQL 8，并在首次启动时执行 `deploy/init.sql`
- `backend`: 构建并启动 Spring Boot 后端
- `nginx`: 构建前端静态站点，并代理 `/api/` 到后端容器

对外只暴露:

- `80` 端口: 前端页面和后端接口统一从这里访问

## 2. 首次上服务器

建议把项目放到固定目录，例如:

```bash
mkdir -p /opt/lottery
cd /opt/lottery
```

然后把本地项目上传到服务器。常见方式:

```bash
scp -r ./Project2 root@你的服务器IP:/opt/lottery/
```

如果你用 git 管理代码，也可以直接在服务器拉取:

```bash
cd /opt
git clone <你的仓库地址> lottery
cd /opt/lottery
```

## 3. 检查服务器环境

在服务器执行:

```bash
docker --version
docker compose version
```

如果 `docker compose` 不可用，再补装 compose 插件。

## 4. 开放端口

至少放行:

- `80/tcp`

如果你后续要接 HTTPS，再放行:

- `443/tcp`

如果服务器启用了 `ufw`:

```bash
ufw allow 80/tcp
ufw allow 443/tcp
ufw reload
```

同时确认云服务器控制台的安全组也放通了 `80/443`。

## 5. 启动项目

进入项目根目录:

```bash
cd /opt/lottery
```

直接启动:

```bash
docker compose up -d --build
```

查看状态:

```bash
docker compose ps
```

查看日志:

```bash
docker compose logs -f
```

单独看某个服务:

```bash
docker compose logs -f mysql
docker compose logs -f backend
docker compose logs -f nginx
```

## 6. 访问方式

启动成功后，浏览器直接访问:

```text
http://你的服务器IP/
```

接口和文档路径:

- 前端: `/`
- Swagger: `/swagger-ui.html`
- OpenAPI: `/v3/api-docs`

例如:

```text
http://你的服务器IP/swagger-ui.html
```

## 7. 数据库初始化说明

`docker-compose.yml` 里已经挂载了:

- `./data/mysql:/var/lib/mysql`
- `./deploy/init.sql:/docker-entrypoint-initdb.d/init.sql:ro`

含义:

- 第一次启动时，MySQL 会执行 `deploy/init.sql`
- 数据会持久化到宿主机的 `data/mysql`

注意:

- `docker-entrypoint-initdb.d` 只会在数据库目录为空时执行
- 如果你改了 `deploy/init.sql`，但已经有旧数据，重启容器不会再次执行

如果你要重新初始化整库:

```bash
docker compose down
rm -rf data/mysql
docker compose up -d --build
```

这会清空数据库数据，只能在确认可以删库时使用。

## 8. 当前默认账号信息

当前仓库里的数据库配置是:

- 数据库名: `lottery_db`
- 用户名: `root`
- 密码: `123456`

对应文件:

- `docker-compose.yml`
- `lottery-backend/src/main/resources/application.yml`

虽然 compose 已通过环境变量覆盖后端数据库地址，但密码仍然是默认值。上线前至少把下面两处一起改掉:

- `docker-compose.yml` 中的 `MYSQL_ROOT_PASSWORD`
- `docker-compose.yml` 中的 `SPRING_DATASOURCE_PASSWORD`

如果要改数据库名或账号，也要同步调整:

- `MYSQL_DATABASE`
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`

## 9. 域名和 HTTPS

如果你只是先验证功能，直接用 `http://服务器IP/` 就够了。

如果你要正式对外使用，建议:

1. 域名解析到服务器公网 IP
2. 使用 Nginx 或网关接入 HTTPS
3. 申请证书，例如 Let's Encrypt

当前项目内部已经有一个容器内 Nginx，最简单的正式方案有两种:

1. 直接修改当前 `lottery-frontend/nginx.conf`，让容器内 Nginx 监听 `443`
2. 保持当前项目不变，在宿主机或外层再放一个反向代理做 HTTPS 终止

如果你只是轻量服务器单项目部署，第二种更稳，后续续签证书也更清晰。

## 10. 后续更新流程

代码更新后，在服务器项目目录执行:

```bash
cd /opt/lottery
docker compose up -d --build
```

如果你确认某个旧镜像需要清理:

```bash
docker image prune -f
```

## 11. 常见问题

### 11.1 页面能打开但接口报错

先看后端日志:

```bash
docker compose logs -f backend
```

重点检查:

- MySQL 是否启动成功
- 后端是否成功连接 `mysql:3306`
- 表是否初始化完成

### 11.2 MySQL 初始化脚本没执行

通常是因为 `data/mysql` 已经存在旧数据。删除数据目录后重建才会重新执行初始化脚本。

### 11.3 上传文件失败

当前 Nginx 已配置:

```nginx
client_max_body_size 50m;
```

后端也配置了:

- `max-file-size: 50MB`
- `max-request-size: 50MB`

如果后续文件超过 50MB，需要同时改前端 Nginx 和后端 Spring 配置。

## 12. 推荐的实际上线命令

如果你现在就要在服务器部署，按这个顺序执行即可:

```bash
cd /opt/lottery
docker compose up -d --build
docker compose ps
docker compose logs --tail=100 mysql
docker compose logs --tail=100 backend
docker compose logs --tail=100 nginx
```

然后访问:

```text
http://你的服务器IP/
```
