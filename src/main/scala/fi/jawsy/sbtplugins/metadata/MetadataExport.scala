package fi.jawsy.sbtplugins.metadata

import scala.xml._
import sbt._

trait ExportBasicDependencyProject extends MetadataExport {
  self: BasicDependencyProject =>
  def metadataConfigurations = List("compile", "provided", "runtime", "test")
  override def metadataXml: NodeSeq = super.metadataXml ++
    <ivy-configurations>
      { metadataConfigurations.map(new Configuration(_)).flatMap(conf =>
        Some(managedClasspath(conf).get).filter(!_.isEmpty).toSeq.map(cp =>
          <configuration name={ conf.name }>
            { cp.flatMap(entry =>
              <classpathEntry path={ entry.absolutePath } />
            )}
          </configuration>
        )
      )}
    </ivy-configurations>
}

trait ExportMavenStyleScalaPaths extends MetadataExport {
  self: MavenStyleScalaPaths =>

  override def metadataXml: NodeSeq = super.metadataXml ++
    <mainCompilePath value={ mainCompilePath.absolutePath } />
    <mainJavaSourcePath value={ mainJavaSourcePath.absolutePath } />
    <mainResourcesPath value={ mainResourcesPath.absolutePath } />
    <mainResourcesOutputPath value={ mainResourcesOutputPath.absolutePath } />
    <mainScalaSourcePath value={ mainScalaSourcePath.absolutePath } />
    <testCompilePath value={ testCompilePath.absolutePath } />
    <testJavaSourcePath value={ testJavaSourcePath.absolutePath } />
    <testResourcesPath value={ testResourcesPath.absolutePath } />
    <testResourcesOutputPath value={ testResourcesOutputPath.absolutePath } />
    <testScalaSourcePath value={ testScalaSourcePath.absolutePath } />
}

trait ExportMavenStyleWebScalaPaths extends MetadataExport {
  self: MavenStyleWebScalaPaths =>
  override def metadataXml: NodeSeq = super.metadataXml ++
    <webappPath value={ webappPath.absolutePath } />
}

trait ExportDefaultWebProject
  extends DefaultWebProject
  with ExportMavenStyleScalaPaths
  with ExportMavenStyleWebScalaPaths
  with ExportBasicDependencyProject {
}

object MetadataExport {
  val ExportMetadataDescription = "Exports project metadata in XML format"
}

trait MetadataExport  {
  self: Project =>

  import MetadataExport._

  def metadataXmlPath = outputPath / "sbt-metadata.xml"
  def metadataXml: NodeSeq = NodeSeq.Empty

  lazy val exportMetadata = exportMetadataAction
  def exportMetadataAction = exportMetadataTask describedAs ExportMetadataDescription
  def exportMetadataTask = task {
    val xml =
      <project organization={ projectOrganization.value } name={ projectName.value } revision={ projectVersion.value.toString }>
        { metadataXml }
      </project>
    FileUtilities.touch(metadataXmlPath, log)
    XML.saveFull(metadataXmlPath.absolutePath, xml, "UTF-8", true, null)
    None
  }

}
