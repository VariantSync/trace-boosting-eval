mvn deploy:deploy-file -DgroupId=org.variantsync -DartifactId=diffdetective -Dversion=2.2.0 -Durl=file:../local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=../src/main/resources/lib/diffdetective-2.2.0-jar-with-dependencies.jar
rm -rf ~/.m2/repository/org/variantsync/
