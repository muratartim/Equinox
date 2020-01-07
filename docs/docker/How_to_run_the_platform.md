## How to run the server-side platform using Docker Compose
1. Download sample data for SFTP server: [sample data](https://drive.google.com/uc?export=download&id=1Ldr7vujbLYqOiaes0DtSNiEDNBN8yHa6)
2. Create directory 'backups' and copy the downloaded 'sftp_data.tar' file into the directory.
3. Create a named volume by executing the following command outside of 'backups' directory:
```docker
docker container run --rm -v equinox_sftp_data:/dbdata -v $(pwd)/backups:/backup ubuntu bash -c "cd /dbdata && tar xvf /backup/sftp_data.tar --strip 1"
```
4. Download sample data for MySQL server: [sample data](https://drive.google.com/uc?export=download&id=1lvzTEUpvwJw7Om-xPynxSe2d3do5f6oz)
5. Create directory 'backups' and copy the downloaded 'mysql_data.tar' file into the directory.
6. Create a named volume by executing the following command outside of 'backups' directory:
```docker
docker container run --rm -v equinox_mysql_data:/dbdata -v $(pwd)/backups:/backup ubuntu bash -c "cd /dbdata && tar xvf /backup/mysql_data.tar --strip 1"
```
7. Create & start the whole platform from where the docker-compose.yml file is located:
```docker
docker-compose up
```
8. Stop & remove the platform from where the docker-compose.yml file is located:
```docker
docker-compose down
```