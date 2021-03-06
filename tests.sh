#!/bin/sh

# cleanup cache dirs & logs
# for filter 
rm -rf ./frontcache-tests/FRONTCACHE_HOME_FILTER/cache/l2-lucene-index/*
rm -rf ./frontcache-tests/FRONTCACHE_HOME_FILTER/logs/*

# for standalone test server 
rm -rf ./frontcache-tests/FRONTCACHE_HOME_STANDALONE/cache/l2-lucene-index/*
rm -rf ./frontcache-tests/FRONTCACHE_HOME_STANDALONE/logs/*
# end cleanup cache dirs & logs

# stop gradle daemons
./gradlew -stop

./gradlew clean :frontcache-tests:startStandaloneFrontcache

./gradlew :frontcache-tests:end2endTests
#./gradlew :frontcache-tests:stopStandaloneFrontcacheByPort

echo "Stoping Standalone Frontcache Server ..."
ps -e | grep standaloneFrontcacheJetty | cut -d' ' -f2 | xargs kill -9
echo "Standalone Frontcache Server has been stoped"
