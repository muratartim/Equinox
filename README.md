# Equinox
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![HitCount](http://hits.dwyl.io/muratartim/Equinox.svg)](http://hits.dwyl.io/muratartim/Equinox)
[![Java Version](https://img.shields.io/badge/java-10-orange.svg)](https://www.oracle.com/technetwork/java/javase/downloads/index.html)

Data analysis and visualisation application for fatigue-digital-twin platform. It is the desktop client of the fatigue-digital-twin platform. Fatigue-digital-twin platform aims at creating a digital fatigue representation of the engineering structure. You can access the platform website at http://www.equinox-digital-twin.com This project contains the prototype desktop client of the platform, named as Equinox. The application has the following major capabilities,

- Parameterized fatigue spectrum analysis,
- 2D & 3D data visualization,
- Search engine for central digital-twin database,
- Collaboration (file and view sharing),
- Automatic report generation,
- Automation API (using XML or JSON instruction files),
- Plugin extensions via automation API.

## Screenshots

Screenshots below demonstrate some of the analysis and data visualization capabilities of Equinox.

<img width="1528" alt="shots" src="https://user-images.githubusercontent.com/13915745/40891639-b25e01c2-6789-11e8-9142-80a913a040b8.png">

## Video demonstrations
Here's a 5 minutes screencast demonstrating some of the analysis and data visualization capabilities of Equinox.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=k49bgTfAgVU
" target="_blank"><img src="http://img.youtube.com/vi/k49bgTfAgVU/0.jpg" 
alt="Equinox Screencast" width="560" /></a>

Here's another video which demonstrates 3D data visualization capabilities of Equinox.

<a href="http://www.youtube.com/watch?feature=player_embedded&v=RM_ofreMsaQ
" target="_blank"><img src="http://img.youtube.com/vi/RM_ofreMsaQ/0.jpg" 
alt="Equinox Screencast" width="560" /></a>

## How to run the project on IDE
You would need to specify the following program and VM arguments in order to run the application in the development environment.

### Main class
```
equinox.Equinox
```

### Program arguments
```
--maxParallelTasks=4 --maxParallelSubtasks=2 --databasePageSize=8192 --databasePageCacheSize=4000 --maxVisibleSTFsPerSpectrum=20 --colorTheme=steelblue
```

### VM arguments
```
-Xms512m -Xmx2048m -XX:+UseStringDeduplication -Xverify:none -server -XX:+UseParallelGC
```

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

## How to deploy the server-side platform on AWS with single EC2 instance
1. Download the Deployment Template File [One-Instance-Arch-CloudFormation.yml](https://github.com/muratartim/Equinox/blob/master/docs/aws/cloud-formation/One-Instance-Arch-CloudFormation.yml).
2. Validate the template by running the following command (from where the Deployment Template File is located):
```
aws cloudformation validate-template --template-body file://./One-Instance-Arch-CloudFormation.yml
```
3. Create an AWS CloudFormation Stack using the following command (from where the Deployment Template File is located). Note that, this command will deploy the platform using the default parameters (which are valid for the 'eu-central-1' AWS Region).
```
aws cloudformation create-stack --stack-name equinox --template-body file://./One-Instance-Arch-CloudFormation.yml
```
4. Delete & rollback the stack as follows:
```
aws cloudformation delete-stack --stack-name equinox
```

This will deploy the platform on AWS utilizing the following architecture:

<img width="1018" alt="Equinox_One_Instance_AWS_Architecture" src="https://user-images.githubusercontent.com/13915745/70265112-0684f280-179a-11ea-8562-5996c8235707.png">

## Full stack server-side deployment on AWS
1. Download the Deployment Template File [Full-Stack-High-Availability-Arch-CloudFormation.yml](https://github.com/muratartim/Equinox/blob/master/docs/aws/cloud-formation/Full-Stack-High-Availability-Arch-CloudFormation.yml). 
2. Validate the template by running the following command (from where the Deployment Template File is located):
```
aws cloudformation validate-template --template-body file://./Full-Stack-High-Availability-Arch-CloudFormation.yml
```
3. Create an AWS CloudFormation Stack using the following command (from where the Deployment Template File is located). Note that, this command will deploy the platform using the default parameters (which are valid for the 'eu-central-1' AWS Region).
```
aws cloudformation create-stack --stack-name equinox-digital-twin --template-body file://./Full-Stack-High-Availability-Arch-CloudFormation.yml
```
4. Delete & rollback the stack as follows:
```
aws cloudformation delete-stack --stack-name equinox-digital-twin
```

This will deploy the platform on AWS utilizing the following architecture:

<img width="1018" alt="Equinox_Full_Stack_AWS_Architecture" src="https://user-images.githubusercontent.com/13915745/71382538-ad5df100-25d8-11ea-9cac-b568d073c469.png">