# from easy to use SFTP server with OpenSSH
FROM atmoz/sftp:alpine

# set maintainer
MAINTAINER Murat Artim "muratartim@gmail.com"

# set user credentials and root directory
ENV SFTP_USERS=aurora:17891917

# copy all files from host to container root directory
# COPY filerRoot home/aurora/filerRoot

# change to user's directory
# WORKDIR /home/aurora

# set all permissions to everyone
# RUN chmod -R ugo+rwx filerRoot

# To create & run the container:
# docker container run -p 2222:22 -d --name equinox_sftp_server --network equinox_network -v equinox_sftp_data:/home/aurora/filerRoot muratartim/equinox_sftp_server

# To start an existing container
# docker container start equinox_sftp_server

# To create a backup from a named volume
# docker container run --rm -v $(pwd)/filerRoot:/filerdata -v $(pwd)/backups:/backup ubuntu tar cvf /backup/sftp_data_"$(date '+%y-%m-%d')".tar /filerdata

# To create a named volume from backup
# assume backup tar file is: $(pwd)/backups/sftp_data.tar
# docker container run --rm -v equinox_sftp_data:/filerdata -v $(pwd)/backups:/backup ubuntu bash -c "cd /filerdata && tar xvf /backup/sftp_data.tar --strip 1"