#!/bin/sh

OS_NAME=$1

OS_LINUX="linux"
OS_WINDOWS="windows"

SRC_DIR="src"

mkdir -p "builds"

# Get JDK for target platform
mkdir -p "builds/jdk"
export JDK_DIR="builds/jdk/$OS_NAME"
if [ ! -d $JDK_DIR ]; then
    if [ $OS_NAME = $OS_LINUX ]; then
        jdk_download="builds/jdk/jdk21.tar.gz"
        wget -O "./$jdk_download" "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jdk_x64_linux_hotspot_21.0.9_10.tar.gz"
        mkdir -p "./$JDK_DIR"
        tar -xzf "./$jdk_download" -C "./$JDK_DIR" --strip-components=1
        rm -f "./$jdk_download"
    fi
    if [ $OS_NAME = $OS_WINDOWS ]; then
        jdk_download="builds/jdk/jdk21.zip"
        wget -O "./$jdk_download" "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.9%2B10/OpenJDK21U-jdk_x64_windows_hotspot_21.0.9_10.zip"
        mkdir -p "./$JDK_DIR"
        unzip "./$jdk_download" -d "./$JDK_DIR"
        mv "./$JDK_DIR"/*/* "./$JDK_DIR"
        rm -f "./$jdk_download"
    fi
fi

# Prepare output directory
OUT_DIR="builds/$OS_NAME"
rm -rf "./$OUT_DIR"
mkdir "./$OUT_DIR"

# Run Gradle build
export VENTURA_BUILD_OS_NAME=$OS_NAME
../gradlew :client:installDist

# Copy Gradle build result into output directory
cp -a "build/install/client/." "./$OUT_DIR"

# Copy game resources into output directory
cp -a "res" "./$OUT_DIR"
cp -a "shaders" "./$OUT_DIR"

# Delete unneeded font files
FONT_DIR="$OUT_DIR/res/fonts"
for font in "$FONT_DIR"/*; do
    font_name=$(basename "$font")
    if ! grep -rq "$font_name" "$SRC_DIR"; then
        rm "$font"
    fi
done

# Copy license file
cp "../LICENSE" "./$OUT_DIR"

# Create usercode directory
mkdir -p "./$OUT_DIR/usercode"

# Create a minimal version of the JRE in the output directory
jlink \
    --module-path "./$JDK_DIR/jmods" \
    --add-modules java.base,java.desktop,jdk.unsupported,java.logging,jdk.crypto.ec \
    --output "./$OUT_DIR/jre" \
    --strip-debug \
    --no-header-files \
    --no-man-pages

# Create a run script in the output directory
if [ $OS_NAME = $OS_LINUX ]; then
    cat > "./$OUT_DIR/run" << 'EOF'
#!/bin/sh
cd $( cd -P -- "$(dirname -- "$(command -v -- "$0")")" && pwd -P )
export JAVA_HOME="$PWD/jre/"
bin/client > "$PWD/latest.log" 2>&1
EOF
    chmod +x "./$OUT_DIR/run"
fi
if [ $OS_NAME = $OS_WINDOWS ]; then
    cat > "./$OUT_DIR/run.bat" << 'EOF'
@echo off
cd /d "%~dp0"
set "JAVA_HOME=%CD%\jre"
powershell -WindowStyle Hidden -Command "Start-Process cmd -ArgumentList \"/c `\"bin\client.bat > latest.log 2>&1`\"\" -WindowStyle Hidden"
EOF
fi