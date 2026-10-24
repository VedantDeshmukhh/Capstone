#!/bin/sh
cd "$(dirname "$0")"
mkdir -p bin
find src -name '*.java' > sources.txt
javac -encoding UTF-8 -d bin @sources.txt || { echo "Compile failed. Install JDK 17+."; exit 1; }
rm -f sources.txt
java -cp bin com.resolvex.AdminMain &
sleep 2
java -cp bin com.resolvex.UserMain &
echo "Admin: http://localhost:8080 (admin/admin123)   User: http://localhost:8081 (cse1/pass123)"
wait
