scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "javax.mail" % "javax.mail-api" % "1.5.5",
  "com.sun.mail" % "javax.mail" % "1.5.5",
  "org.mnode.mstor" % "mstor" % "0.9.13",
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "com.typesafe" % "config" % "1.3.0",
  "org.rogach" %% "scallop" % "1.0.1",
  "pl.project13.scala" %% "rainbow" % "0.2"
)
