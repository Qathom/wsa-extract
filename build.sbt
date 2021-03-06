name := "wsa-extract-2017"

version := "1.0"

scalaVersion := "2.11.0"

val sparkVersion = "2.1.0"

libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion
)

libraryDependencies += "org.codehaus.jettison" % "jettison" % "1.3.7"

libraryDependencies += "com.databricks" % "spark-csv_2.11" % "1.2.0"
