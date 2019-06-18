前端代码build.gradle

plugins {
    id "com.moowork.node" version "1.1.1"
    id 'java'
}

group 'com.yonyoucloud.ec'
version '1.0.0-SNAPSHOT'

sourceCompatibility = 1.8

static def gitBranch() {
    def branch = ""
    def proc = "git rev-parse --abbrev-ref HEAD".execute()
    proc.in.eachLine { line -> branch = line }
    proc.err.eachLine { line -> println line }
    proc.waitFor()
    branch
}

def branch = gitBranch()

def npmEnv = "test"
if ("release" == branch) {
    npmEnv = "prev"
} else if ("master" == branch) {
    npmEnv = "build"
}
//调用npm run build命令的Gradle任务
task npmDev(type: NpmTask, dependsOn: npmInstall) {
    group = 'node'
    args = ['run', "$npmEnv"]
}

task copyFront(type: Copy, dependsOn: npmDev) {
    from "$buildDir/../dist/"
    into "$buildDir/../../ticket-server/build/resources/main/static/"
    include '**'
}

//Gradle的java插件的jar任务，依赖npmBuild,即web子模块打jar包前必须运行npm run build
jar.dependsOn copyFront


后端代码需要在build.gradle中进行触发，将前后端代码打包到一起
compile project(':ws-console-front')