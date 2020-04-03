# OpenVPNWebUI
Simple web UI for OpenVPN

# Простая веб-морда для OpenVPN сервера
```
Написана на Kotlin + Spring Boot 2 + AngularJS.
Для авторизации использует Keycloak.
```

# Как установить
 - Ставим докер
 - Ставим в докер Рostgres и Keycloak, не забыв поправить настройки:
```
docker run --name postgres -v /docker/data/postgresql/data:/var/lib/postgresql/data -v /docker/data/postgresql/init:/docker-entrypoint-initdb.d -e POSTGRES_USER=pgadmin -e POSTGRES_PASSWORD=pgadmin -p 127.0.0.1:5432:5432  -d postgres

docker run --name keycloak -p 127.0.0.1:8000:8080 -e KEYCLOAK_USER=root -e KEYCLOAK_PASSWORD=12345 -e DB_USER=keycloak -e DB_PASSWORD=keycloak -e DB_ADDR=127.0.0.1 -e DB_VENDOR=postgres -e PROXY_ADDRESS_FORWARDING=true -e jboss.https.port=443 -d jboss/keycloak
```
 - создаем realm и пользователей в keycloak, правим в проекте application.yml в соответствии со сделанными настройками.
 - если докер локальный, то делаем mvn clean install - оно соберет проект и сразу все задеплоит в докер.
 - если докер на VPS, то mvn clean package, дальше копируем папку docker на VPS и там из нее запускаем ./deploy.sh openvpn-ui 1.0
 
 # Зачем лежат готовые кейсторы?
  - Один нужен для поднятия https на локалхосте. Самоподписанный.
  - Второй содержит сертификаты бесплатного СА letsencrypt, поскольку не везде они есть по-умолчанию. Точнее, они есть в браузерах везде, а вот в хранилище java их может не быть. Сложил в гит, чтобы скачавшему проект не морочить голову с их генерацией.
  
![1](https://user-images.githubusercontent.com/8249779/78343656-d9a7e700-75a3-11ea-951d-b0143bf59e7a.png)
![2](https://user-images.githubusercontent.com/8249779/78343661-dad91400-75a3-11ea-8e48-9e5713e9fe95.png)
