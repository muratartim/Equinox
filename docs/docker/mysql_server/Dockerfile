# from latest version of MySQL
FROM mysql:latest

# set maintainer
MAINTAINER Murat Artim "muratartim@gmail.com"

# set user credentials and root directory
ENV MYSQL_ROOT_PASSWORD=Martim_17891917
ENV MYSQL_DATABASE=AFTwin
ENV MYSQL_USER=martim
ENV MYSQL_PASSWORD=Martim_17891917

# copy custom configuration file
COPY my.cnf /etc/mysql/my.cnf

# build image
# docker image build -t muratartim/equinox_mysql_server:latest .

# To create & run the container:
# docker container run -p 3306:3306 -d --name equinox_mysql_server --network equinox_network -v equinox_mysql_data:/var/lib/mysql muratartim/equinox_mysql_server

# To start an existing container
# docker container start equinox_mysql_server

# To create a backup from a named volume
# docker container run --rm -v equinox_mysql_data:/dbdata -v $(pwd)/backups:/backup ubuntu tar cvf /backup/mysql_data.tar /dbdata

# To create a named volume from backup
# assume backup tar file is: $(pwd)/backups/mysql_data.tar
# docker container run --rm -v equinox_mysql_data:/dbdata -v $(pwd)/backups:/backup ubuntu bash -c "cd /dbdata && tar xvf /backup/mysql_data.tar --strip 1"