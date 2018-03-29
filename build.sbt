import akka.grpc.gen.scaladsl.ScalaBothCodeGenerator

name := """play-grpc-scala"""
organization := "com.example"

version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
    .enablePlugins(PlayScala)
    .enablePlugins(AkkaGrpcPlugin)


(akkaGrpcCodeGenerators in Compile) := Seq(GeneratorAndSettings(ScalaBothCodeGenerator, (akkaGrpcCodeGeneratorSettings in Compile).value))

scalaVersion := "2.12.4"

libraryDependencies += guice
libraryDependencies += "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2" % Test

// for loading of cert, issue #89
libraryDependencies += "io.grpc" % "grpc-testing" % "1.10.0"

// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.example.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.example.binders._"

