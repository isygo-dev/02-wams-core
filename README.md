# 02-wams-core

> WAMS Core microservices.

# Technology Stack

 Component       | Technology 
-----------------|------------
 Java            | 17         
 Spring Boot     | 3.2.1      
 Spring security | 6.1.1      
 Postgres SQL    | 17         
 Hibernate       | 6.2.2      
 Maven           | 3          

# Getting Started

## Prerequisites

- Install Git last version from https://git-scm.com/downloads/win
- Install Intellij IDEA last version (Ultimate or Community) from https://www.jetbrains.com/idea/download/other.html
- Install Java 17 from https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- Install Docker desktop last version from https://www.docker.com/products/docker-desktop

### WAMS Core services

- Clone the WAMS Core project from https://github.com/your-org/02-wams-core.git
- Open the project with Intellij IDEA (open as maven project)
- Copy the settings.xml file from the root directory to the Maven .m2 directory
- Run: `mvn clean install` in the terminal or use the Intellij Maven GUI
- Create run configuration for all the services Starter and set the working directory to the related Target directory
  (Ultimate version will detect automatically the Springboot configurations)
- Run configurations in the order: Kms, Ims, Mms, Dms, Sms, Cms

### Docker image

- Run Docker desktop
- Run: docker build -f .\09-docker\core-kms.Dockerfile -t core/kms-serv .
- Run: docker build -f .\09-docker\core-mms.Dockerfile -t core/mms-serv .
- Run: docker build -f .\09-docker\core-ims.Dockerfile -t core/ims-serv .
- Run: docker build -f .\09-docker\core-dms.Dockerfile -t core/dms-serv .
- Run: docker build -f .\09-docker\core-sms.Dockerfile -t core/sms-serv .
- Run: docker build -f .\09-docker\core-cms.Dockerfile -t core/cms-serv .