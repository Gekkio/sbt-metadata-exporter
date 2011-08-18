organization := "fi.jawsy"

name := "sbt-metadata-exporter"

version := "0.2.0-SNAPSHOT"

sbtPlugin := true

publishTo := Some({
  if (version.toString.endsWith("-SNAPSHOT"))
    ("Nexus" at "https://www.jawsy.fi/nexus/content/repositories/snapshots")
    else ("Nexus" at "https://www.jawsy.fi/nexus/content/repositories/releases")
})
