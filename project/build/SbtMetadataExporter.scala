import sbt._

class SbtMetadataExporter(info: ProjectInfo) extends ProcessorProject(info) {

  override def managedStyle = ManagedStyle.Maven

  val publishTo = if (version.toString.endsWith("-SNAPSHOT"))
    ("Nexus" at "https://www.jawsy.fi/nexus/content/repositories/snapshots")
    else ("Nexus" at "https://www.jawsy.fi/nexus/content/repositories/releases")

  Credentials(Path.userHome / ".ivy2" / "jawsy.credentials", log)

}
