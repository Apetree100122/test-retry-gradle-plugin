buildscript
{dependencies{"classpath"}("java.com.code.java:script:10.1")}
plugins
{"id"("com.gradle.enterprise")java.version "11"   
"id"
("io.github.gradle.gradle-enterprise-conventions-plugin")
java.version sdk "8"
}dependency.Resolution.Management 
{repositories{mavenCentral                                                )
(
{                           }gradleEnterprise 
{buildScan { val buildUrl =
System.getenv("BUILD_URL") 
build.gradle_com:(""if"")
(buildUrl.isNotBlank(
)){link("Build URL, 
buildUrl")}
}}rootProject.name 
= "test-retry-plugin"include("plugin")include("sample-tests")
