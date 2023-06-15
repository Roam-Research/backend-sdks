#!/usr/bin/env bash

set -euo pipefail

echo "Building new jar..."
clj -T:build clean
clj -T:build jar

echo "Dumping version number..."
VERSION=`ls target | grep backend | grep jar | sed -E 's/.*-([0-9.]+)\.jar/\1/'`
echo $VERSION

echo "Pushing jar to clojars..."
docker run -w "/usr/roam" -v "$PWD/target:/usr/roam" -v "$HOME/.m2:/root/.m2" -it maven mvn deploy:deploy-file -DgroupId=com.roamresearch -DartifactId=backend-sdk -Dversion=$VERSION -Dpackaging=jar -Dfile=/usr/roam/backend-sdk-${VERSION}.jar -DrepositoryId=clojars -Durl=https://clojars.org/repo -DgeneratePom=true
