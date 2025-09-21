#!/bin/bash

echo "🔨 开始构建Android APK..."

# 设置环境变量
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 检查环境
echo "📋 检查构建环境..."
if [ ! -d "$ANDROID_HOME" ]; then
    echo "❌ Android SDK 未找到"
    exit 1
fi

echo "✅ Android SDK: $ANDROID_HOME"
echo "✅ Java版本: $(java -version 2>&1 | head -n 1)"

# 清理项目
echo "🧹 清理项目..."
./gradlew clean

# 构建Debug APK
echo "📱 构建Debug APK..."
./gradlew assembleDebug

# 检查构建结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ 构建成功！"
    echo "📦 APK位置: app/build/outputs/apk/debug/app-debug.apk"
    ls -lh app/build/outputs/apk/debug/app-debug.apk
else
    echo "❌ 构建失败，检查输出信息"
    exit 1
fi

echo "🎉 构建完成！"