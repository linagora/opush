## HOW TO BUILD A VERSION
Assume that we are building the 3.0.0~beta2 version.

#### 1) CREATE JIRA A TICKET
In this doc we assume that the created jira is OP-1234

#### 2) TAG POM FILES
Be on the latest master version  

    git fetch origin
    git branch -D master
    git checkout origin/master -b master

Tag pom files  

    find . -type f -name pom.xml -exec sed -i 's!3.0.0-SNAPSHOT!3.0.0-beta2!' {} \;

In the main pom.xml search for:
- The xml tag "opush.packaging.version" and remove the tailing ~git$  
  You should get <opush.packaging.version>${opush.release}~beta2</opush.packaging.version>  
- The xml tag "opush.version" and change the -SNAPSHOT to -beta2
  You should get <opush.version>${opush.release}-beta2</opush.version>

    git commit -a -m "OP-1234 Bump version to 3.0.0-beta2"

#### 3) CREATE GIT TAG
    git tag 3.0.0-beta2

#### 4) REVERT THE TAG OF POM FILES
    git revert --no-edit HEAD

#### 5) SET THE NEXT VERSION TO WORK ON
In opush.packaging.version, replace beta2 by beta3  

    git commit -a -m "OP-1234 Bump version to 3.0.0~beta3~git"

#### 6) PUSH CHANGES
    git push origin master --tags

#### 7) LAUNCH THE JENKINS BUILD
go to https://ci-obm.linagora.com/jenkins/view/1-package-release/job/opush-release/build?delay=0sec

    TAG = 3.0.0~beta2
    TARGET_REPO_NAME = testing

