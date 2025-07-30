@echo off
echo "=== Gradle Dependencies Check ==="
gradlew dependencies --configuration compileClasspath | findstr redis
echo "=== Gradle Build ==="
gradlew compileJava
pause