mvn deploy:deploy-file -DgroupId=org.variantsync.vevos -DartifactId=simulation -Dversion=2.0.0 -Durl=file:../local-maven-repo/ -DrepositoryId=local-maven-repo -DupdateReleaseInfo=true -Dfile=../src/main/resources/lib/simulation-2.0.0-jar-with-dependencies.jar
rm -rf ~/.m2/repository/org/variantsync/vevos/
