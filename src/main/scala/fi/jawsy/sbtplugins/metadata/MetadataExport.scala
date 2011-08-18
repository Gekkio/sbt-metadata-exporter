package fi.jawsy.sbtplugins.metadata

import scala.xml._
import sbt._
import sbt.processor._

trait ExportBasicDependencyProject extends MetadataExport {
  override type P <: BasicDependencyProject
  val metadataConfigurations = List("compile", "provided", "runtime", "test")

  override def description = "Managed classpath" :: super.description
  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
    <ivy-configurations>
      { metadataConfigurations.map(new Configuration(_)).flatMap(conf =>
        Some(projectClasspath(conf).get).filter(!_.isEmpty).toSeq.map(cp =>
          <configuration name={ conf.name }>
            { cp.flatMap(entry =>
              <classpathEntry path={ entry.relativePath } />
            )}
          </configuration>
        )
      )}
    </ivy-configurations>
  }
}

trait ExportMavenStyleScalaPaths extends MetadataExport {
  override type P <: MavenStyleScalaPaths

  override def description = "Maven-style Scala paths" :: super.description

  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
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
}
trait ExportMavenStyleWebScalaPaths extends MetadataExport {
  override type P <: MavenStyleWebScalaPaths

  override def description = "Web-app path" :: super.description

  override def metadataXml: NodeSeq = {
    import project._
    super.metadataXml ++
    <webappPath value={ webappPath.relativePath } />
  }
}

class MetadataExportProcessor extends BasicProcessor {

  implicit def defaultWebProject(p: DefaultWebProject) =
    new ExportProject(p, "DefaultWebProject")
    with ExportMavenStyleScalaPaths
    with ExportMavenStyleWebScalaPaths
    with ExportBasicDependencyProject

  implicit def defaultProject(p: DefaultProject) =
    new ExportProject(p, "DefaultProject")
    with ExportMavenStyleScalaPaths
    with ExportBasicDependencyProject

  implicit def project(p: Project) = new ExportProject(p, "Project")

  def apply(project: Project, args: String) {
    project match {
      case p: DefaultWebProject => p.exportMetadata
      case p: DefaultProject => p.exportMetadata
      case p: ParentProject => p.exportMetadata
      case p: Project => p.exportMetadata
    }
    project.subProjects.map(_._2).foreach(apply(_, ""))
  }
}

class ExportProject[Pr <: Project](val project: Pr, projectType: String) extends MetadataExport {
  override type P = Pr
  override def exportMetadata {
    project.log.info(project + " looks like a " + projectType + ". These will be exported:")
    description.foreach(s => project.log.info("* " + s))
    super.exportMetadata
  }
}

trait MetadataExport {
  val project: P
  type P <: Project

  def metadataXml: NodeSeq = {
    val deps = project.dependencies
    if (deps.isEmpty) NodeSeq.Empty
    else
      <dependencies>
        { deps.map(dep =>
          <dependency name={ dep.projectName.value } organization={ dep.projectOrganization.value } version={ dep.projectVersion.value.toString } />
        )}
      </dependencies>
  }

  def description: List[String] = "Basic project details and project-to-project dependencies" :: Nil

  def exportMetadata {
    import project._

    val xml =
      <project name={projectName.value} organization={ projectOrganization.value } version={ projectVersion.value.toString } scalaVersion={
      buildScalaVersion } metadataVersion="1">
        { metadataXml }
      </project>
    val metadataXmlPath = outputPath / "sbt-metadata.xml"
    FileUtilities.touch(metadataXmlPath, log)
    XML.saveFull(metadataXmlPath.absolutePath, xml, "UTF-8", true, null)
    log.success("Wrote metadata to " + metadataXmlPath)
  }

}
