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
              <classpathEntry path={ entry.relativePath } />
            )}
          </configuration>
        )
      )}
    </ivy-configurations>
}

trait ExportMavenStyleScalaPaths extends MetadataExport {
  self: MavenStyleScalaPaths =>

  override def metadataXml: NodeSeq = super.metadataXml ++
    <mainCompilePath value={ mainCompilePath.relativePath } />
    <mainJavaSourcePath value={ mainJavaSourcePath.relativePath } />
    <mainResourcesPath value={ mainResourcesPath.relativePath } />
    <mainResourcesOutputPath value={ mainResourcesOutputPath.relativePath } />
    <mainScalaSourcePath value={ mainScalaSourcePath.relativePath } />
    <testCompilePath value={ testCompilePath.relativePath } />
    <testJavaSourcePath value={ testJavaSourcePath.relativePath } />
    <testResourcesPath value={ testResourcesPath.relativePath } />
    <testResourcesOutputPath value={ testResourcesOutputPath.relativePath } />
    <testScalaSourcePath value={ testScalaSourcePath.relativePath } />
}

trait ExportMavenStyleWebScalaPaths extends MetadataExport {
  self: MavenStyleWebScalaPaths =>
  override def metadataXml: NodeSeq = super.metadataXml ++
    <webappPath value={ webappPath.relativePath } />
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
      <project name={projectName.value} organization={ projectOrganization.value } version={ projectVersion.value.toString }>
        { metadataXml }
      </project>
    FileUtilities.touch(metadataXmlPath, log)
    XML.saveFull(metadataXmlPath.relativePath, xml, "UTF-8", true, null)
    None
  }

}
