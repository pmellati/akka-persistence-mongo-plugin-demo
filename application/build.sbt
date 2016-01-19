name := "Mongo Journal Demo"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

libraryDependencies ++= {
  def akka(depName: String) = "com.typesafe.akka" %% depName % "2.4.1"

  Seq(
    akka("akka-actor"),
    akka("akka-slf4j"),
    akka("akka-remote"),
    akka("akka-persistence"),
    akka("akka-cluster-sharding"),
    akka("akka-testkit"),

    "com.github.scullxbones" %% "akka-persistence-mongo-rxmongo" % "1.1.8",
    "org.reactivemongo" %% "reactivemongo" % "0.11.7"   // The akka persistence mongo plugin is incompatible with reactivemongo versions higher than 0.11.7.
  )
}

resolvers ++= Seq(
  "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
  "dnvriend at bintray" at "http://dl.bintray.com/dnvriend/maven",
  "Typesafe repository releases" at "http://repo.typesafe.com/typesafe/releases/"
)
