name := "cats-effect-interop-twitter"
organization := "com.github.yaroot"
scalaVersion := "2.13.3"
crossScalaVersions := Seq("2.12.11", "2.13.3")

fork in run := true

libraryDependencies ++= {
  Seq(
    "org.typelevel"  %% "cats-effect"                  % "2.3.1",
    "com.twitter"    %% "util-core"                    % "20.12.0",
    "io.monix"       %% "minitest"                     % "2.9.1",
    "com.codecommit" %% "cats-effect-testing-minitest" % "0.5.0"
  )
}

scalafmtOnCompile := true
cancelable in Global := true

testFrameworks += new TestFramework("minitest.runner.Framework")

version ~= (_.replace('+', '-'))
dynver ~= (_.replace('+', '-'))
